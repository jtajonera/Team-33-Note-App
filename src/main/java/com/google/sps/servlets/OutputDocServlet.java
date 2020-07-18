package com.google.sps.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/output-doc")
public class OutputDocServlet extends HttpServlet {

  // TODO: Load signed url download link from Cloud Storage
  // Need to figure out way to pass SessionKeyId to this servlet
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }
}