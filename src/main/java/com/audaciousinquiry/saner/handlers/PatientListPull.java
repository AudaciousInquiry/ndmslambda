package com.audaciousinquiry.saner.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PatientListPull implements RequestHandler<Void, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String handleRequest(Void unused, Context context) {
        LambdaLogger logger = context.getLogger();
        String returnValue;

        logger.log("PatientListPull Lambda - Started");


        return "";
    }
}
