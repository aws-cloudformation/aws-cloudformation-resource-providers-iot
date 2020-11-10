package com.amazonaws.iot.scheduledaudit;

import java.util.Map;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-iot-scheduledaudit.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(ResourceModel resourceModel) {
        if (resourceModel.getTags() == null) {
            return null;
        } else {
            return resourceModel.getTags().stream()
                    .collect(Collectors.toMap(Tag::getKey, Tag::getValue));
        }
    }
}
