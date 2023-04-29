package com.github.crutcher.workshop.json;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Data
public class JsonExample {
    private int id;
}
