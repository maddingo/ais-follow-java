package no.maddin.ais.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class MarineTrafficError {

    private List<Error> errors;

    @Data
    public static class Error {
        private String code;
        private String detail;
    }
}
