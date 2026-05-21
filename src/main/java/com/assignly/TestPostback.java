package com.assignly;

import com.assignly.service.PortalRepository;

public class TestPostback {
    public static void main(String[] args) throws Exception {
        PortalRepository repo = new PortalRepository();
        // Login first
        System.out.println("Logging in...");
        Object res = repo.login("SP25-BCS-136", "136");
        System.out.println("Login: " + res);
        
        System.out.println("Fetching summary...");
        String summary = repo.fetchPageHtml("Summary.aspx");
        System.out.println("Summary size: " + (summary != null ? summary.length() : "null"));
        
        System.out.println("Posting back...");
        String eventTarget = "ctl00$DataContent$gvCourseSummary$ctl02$lbCourse";
        String postResult = repo.postbackEvent("Summary.aspx", eventTarget);
        if (postResult != null) {
            System.out.println("Postback size: " + postResult.length());
            System.out.println("Postback starts with: " + postResult.substring(0, Math.min(200, postResult.length())));
        } else {
            System.out.println("Postback failed (null)");
        }
    }
}
