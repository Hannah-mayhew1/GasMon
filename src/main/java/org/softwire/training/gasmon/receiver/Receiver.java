package org.softwire.training.gasmon.receiver;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.gasmon.model.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Receiver {

    private static final Logger LOG = LoggerFactory.getLogger(Receiver.class);

    private final AmazonSQS sqs;
    private final String queueUrl;
    private final Gson gson;

    public Receiver(AmazonSQS sqs, String queueUrl) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.gson = createGson();
    }

    public List<Message> getMessages() {
        ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl).withWaitTimeSeconds(1);
        List<Message> messages = sqs.receiveMessage(request).getMessages();
        LOG.debug("Received {} messages", messages.size());
        deleteMessagesFromQueue(messages);
        return messages;
    }

    public List<Event> getEvents() {
        List<Message> rawMessages = getMessages();
        List<Event> convertedEvents = new ArrayList<>();
        for (Message message : rawMessages) {
            Event convertedEvent = gson.fromJson(message.getBody(), Event.class);
            convertedEvents.add(convertedEvent);
        }
        return convertedEvents;
    }

    private void deleteMessagesFromQueue(List<Message> messages) {
        if (messages.size() > 0) {
            List<DeleteMessageBatchRequestEntry> deleteMessageBatchRequestEntries = Streams.zip(
                    messages.stream(),
                    IntStream.range(0, messages.size()).boxed(),
                    (message, i) -> new DeleteMessageBatchRequestEntry(i.toString(), message.getReceiptHandle())
            ).collect(Collectors.toList());
            LOG.debug("Deleting messages with request {}", deleteMessageBatchRequestEntries);
            DeleteMessageBatchResult result = sqs.deleteMessageBatch(queueUrl, deleteMessageBatchRequestEntries);
            result.getFailed().forEach(failure -> LOG.warn("Failed to delete SQS message: {}", failure));
            LOG.debug("Deleted {} of {} messages", messages.size() - result.getFailed().size(), messages.size());
        }
    }

    private Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Event.class, (JsonDeserializer<Event>) (jsonElement, type, jsonDeserializationContext) -> {
            JsonElement event = jsonElement.getAsJsonObject().get("Message");
            String unescapedEvent = event.toString().replaceAll("^\"|\"$|\\\\", "");
            return new Gson().fromJson(unescapedEvent, Event.class);
        });
        return gsonBuilder.create();
    }
}
