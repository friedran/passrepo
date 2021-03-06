package com.example.passrepo;

import java.io.IOException;

import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.passrepo.drive.Constants;
import com.example.passrepo.util.Logger;
import com.example.passrepo.util.QueryStringParser;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;

public class GoogleAuthWebViewClient extends WebViewClient {
    
    private GoogleAuthorizationCodeFlow flow;
    private GoogleAuthWebViewClientCallback callback;
    private boolean isFinishedRedirect;
        
    public GoogleAuthWebViewClient(GoogleAuthorizationCodeFlow flow, GoogleAuthWebViewClientCallback callback) {
        this.flow = flow;
        this.callback = callback;
        this.isFinishedRedirect = false;
    }        

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        System.out.println("page started");
        super.onPageStarted(view, url, favicon);
    }            
    
    @Override
    public void onPageFinished(WebView view, final String url) {
        System.out.println("page finished");
        
        if (!url.startsWith("http://local")) {
            super.onPageFinished(view, url);
            return;
        }
        
        view.setVisibility(View.INVISIBLE);
        
        // Avoid multiple parallel callbacks (happen for some reason?)
        synchronized(this) {
            if (isFinishedRedirect) {
                return;
            }
            
            isFinishedRedirect = true;
        }
        
        // Send the token request (must be asynchronously so not to interfere main thread).
        new Thread(new Runnable() {
            @Override
            public void run() {
                String authorizationCode = extractParamFromUrl(url, "code");
                
                GoogleAuthorizationCodeTokenRequest tokenRequest =
                        flow.newTokenRequest(authorizationCode).setRedirectUri(Constants.REDIRECT_URI);

                try {
                    GoogleTokenResponse gtr = tokenRequest.execute(); 
                    flow.createAndStoreCredential(gtr, "");
                    callback.onSuccess();
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onError();
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }    

    private static String extractParamFromUrl(String url,String paramName) {
        String queryString = url.substring(url.indexOf("?", 0)+1,url.length());
        QueryStringParser queryStringParser = new QueryStringParser(queryString);
        return queryStringParser.getQueryParamValue(paramName);
    }
}
