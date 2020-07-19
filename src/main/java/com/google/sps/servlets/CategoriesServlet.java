package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import com.google.gson.Gson;

/** Servlet that returns a list of categories. */
@WebServlet("/categories")
public class CategoriesServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query query = new Query("Note").addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);

    Set<String> allCategories = new HashSet<>();
    for (Entity entity : results.asIterable()) {
      ArrayList<String> categories;
      
      if (entity.getProperty("categories") == null) {
        categories = new ArrayList<>();
      } else {
        categories = (ArrayList) entity.getProperty("categories");
      }
      
      for (String category : categories) {
        if (!allCategories.contains(category)){
          allCategories.add(category);
        }
      }
    }
    
    // Convert the list of categories to JSON.
    Gson gson = new Gson();
    String json = gson.toJson(allCategories);

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }
}