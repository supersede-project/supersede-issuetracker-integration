/*
   (C) Copyright 2015-2018 The SUPERSEDE Project Consortium
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package it.eu.supersede.jira.plugins.servlet;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

import static org.junit.Assert.*;


public class SupersedeCfgFuncTest {

    HttpClient httpClient;
    String baseUrl;
    String servletUrl;

    @Before
    public void setup() {
        httpClient = new DefaultHttpClient();
        baseUrl = System.getProperty("baseurl");
        servletUrl = baseUrl + "/plugins/servlet/supersedecfg";
    }

    @After
    public void tearDown() {
        httpClient.getConnectionManager().shutdown();
    }

    @Test
    public void testSomething() throws IOException {
        HttpGet httpget = new HttpGet(servletUrl);

        // Create a response handler
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpClient.execute(httpget, responseHandler);
        assertTrue(null != responseBody && !"".equals(responseBody));
    }
}
