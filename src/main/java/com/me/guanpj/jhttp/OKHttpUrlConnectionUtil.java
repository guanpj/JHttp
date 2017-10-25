package com.me.guanpj.jhttp;

import android.webkit.URLUtil;

import com.me.guanpj.jhttp.error.AppException;
import com.me.guanpj.jhttp.listener.OnProgressUpdateListener;
import com.me.guanpj.jhttp.upload.UploadUtil;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

/**
 * Created by Jie on 2017/4/8.
 */

public class OKHttpUrlConnectionUtil {
    private static OkHttpClient mClient;

    public static HttpURLConnection execute(Request request, OnProgressUpdateListener listener) throws AppException {
        if (!URLUtil.isNetworkUrl(request.url)) {
            throw new AppException(AppException.ErrorType.MANUAL, "the url : " + request.url + " is invalid!");
        }
        if (mClient == null) {
            mClient = new OkHttpClient();
        }
        switch (request.method) {
            case GET:
            case DELETE:
                return get(request);
            case POST:
            case PUT:
                return post(request, listener);
        }
        return null;
    }

    public static HttpURLConnection get(Request request) throws AppException {
        try {
            request.checkIfCancelled();
            HttpURLConnection connection = new OkUrlFactory(mClient).open(new URL(request.url));
            connection.setRequestMethod(request.method.name());
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            addHeader(connection, request.headers);
            request.checkIfCancelled();
            return connection;
        } catch (MalformedURLException e) {
            throw new AppException(AppException.ErrorType.SERVER, e.getMessage());
        } catch (ProtocolException e) {
            throw new AppException(AppException.ErrorType.SERVER, e.getMessage());
        }
    }

    public static HttpURLConnection post(Request request, OnProgressUpdateListener listener) throws AppException {
        HttpURLConnection connection = null;
        OutputStream os = null;
        try {
            request.checkIfCancelled();
            connection = new OkUrlFactory(mClient).open(new URL(request.url));
            connection.setRequestMethod(request.method.name());
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);

            addHeader(connection, request.headers);

            request.checkIfCancelled();

            os = connection.getOutputStream();
            if (request.filePath != null) {
                UploadUtil.upload(os, request.filePath);
            } else if (request.fileEntities != null) {
                UploadUtil.upload(os, request.content, request.fileEntities, listener);
            } else if (request.content != null) {
                os.write(request.content.getBytes());
            } else {
                throw new AppException(AppException.ErrorType.MANUAL, "the post request has no post content");
            }

            request.checkIfCancelled();
        } catch (InterruptedIOException e) {
            throw new AppException(AppException.ErrorType.TIMEOUT, e.getMessage());
        } catch (IOException e) {
            throw new AppException(AppException.ErrorType.SERVER, e.getMessage());
        } finally {
            try {
                os.flush();
                os.close();
            } catch (IOException e) {
                throw new AppException(AppException.ErrorType.IO, "the post outputstream can't be closed");
            }
        }

        return connection;
    }

    private static void addHeader(HttpURLConnection connection, Map<String, String> headers) {
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

}
