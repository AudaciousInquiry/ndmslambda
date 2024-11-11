package com.audaciousinquiry.saner.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class Job {
    private String id;
    private String status;
    private String type;
    private Date created;
    private Date lastUpdated;
    private List<JobNote> notes;
}
