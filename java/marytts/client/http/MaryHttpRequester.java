/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.client.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

import marytts.util.string.StringUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.BufferingHttpClientHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.HttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

/**
 * @author oytun.turk
 *
 */
            
public class MaryHttpRequester 
{
    public MaryHttpRequester()
    {
         
    }
    
    //TO DO
    public String[] requestMultipleLines(String host, int port, String strRequest)
    {
        return null;
    }
    
    public String requestSingleLine(String host, int port, String strRequest)
    {
        return null;
    }
   
    public InputStream requestInputStream(String host, int port, String strRequest) throws Exception
    {
        HttpResponse response = request(host, port, strRequest);
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent(); //This is how to get a direct input stream from server. Mary client can use this as audio data source from server etc
        
        return is;
    }
    
    
    
    
    
    
    
    
    
    
    
    public HttpResponse request(String host, int port, String strRequest) throws Exception
    {
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 50000)
            .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setParameter(CoreProtocolPNames.USER_AGENT, "HttpComponents/1.1");

        final ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(2, params);

        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        httpproc.addInterceptor(new RequestConnControl());
        httpproc.addInterceptor(new RequestUserAgent());
        httpproc.addInterceptor(new RequestExpectContinue());
        
        int numRequests = 1;
        // We are going to use this object to synchronize between the 
        // I/O event and main threads
        CountDownLatch requestCount = new CountDownLatch(numRequests);
        
        MaryHttpRequestExecutionHandler maryReqExeHandler = new MaryHttpRequestExecutionHandler(requestCount);
        
        BufferingHttpClientHandler handler = new BufferingHttpClientHandler(
                httpproc,
                maryReqExeHandler,
                new DefaultConnectionReuseStrategy(),
                params);

        handler.setEventListener(new EventLogger());
        
        final IOEventDispatch ioEventDispatch = new DefaultClientIOEventDispatch(handler, params);
        
        Thread t = new Thread(new Runnable() {
         
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (InterruptedIOException ex) {
                    System.err.println("Interrupted");
                } catch (IOException e) {
                    System.err.println("I/O error: " + e.getMessage());
                }
                System.out.println("Shutdown");
            }
            
        });
        t.start();

        SessionRequest[] reqs = new SessionRequest[numRequests];

        /*
        reqs[0] = ioReactor.connect(
                new InetSocketAddress("localhost", 59125), 
                null,
                new HttpHost("localhost:59125"),
                new MySessionRequestCallback(requestCount));
                */
        
        reqs[0] = ioReactor.connect(
                new InetSocketAddress(host, port), 
                null,
                new HttpHost(host + ":" + String.valueOf(port) + "?" + strRequest),
                new MySessionRequestCallback(requestCount));
        
        // Block until all connections signal
        // completion of the request execution
        requestCount.await();
        
        System.out.println("Shutting down I/O reactor");
        
        ioReactor.shutdown();
        
        System.out.println("Done");
        
        return maryReqExeHandler.responseOut;
    }
    
    static class MaryHttpRequestExecutionHandler implements HttpRequestExecutionHandler 
    {
        public HttpResponse responseOut;
        
        private final static String REQUEST_SENT       = "request-sent";
        private final static String RESPONSE_RECEIVED  = "response-received";
        
        private final CountDownLatch requestCount;
        
        public MaryHttpRequestExecutionHandler(final CountDownLatch requestCount) {
            super();
            this.requestCount = requestCount;
        }
        
        public void initalizeContext(final HttpContext context, final Object attachment) {
            HttpHost targetHost = (HttpHost) attachment;
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, targetHost);
        }
        
        public void finalizeContext(final HttpContext context) {
            Object flag = context.getAttribute(RESPONSE_RECEIVED);
            if (flag == null) {
                // Signal completion of the request execution
                requestCount.countDown();
            }
        }

        public HttpRequest submitRequest(final HttpContext context) 
        {
            HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            Object flag = context.getAttribute(REQUEST_SENT);
            if (flag == null) {
                // Stick some object into the context
                context.setAttribute(REQUEST_SENT, Boolean.TRUE);

                System.out.println("--------------");
                System.out.println("Sending request from client to server " + targetHost.toURI());
                System.out.println("--------------");
               
                String command = "getAvailableVoices&" + "setAudioEffects=Robot(amount=50.0)";
                
                //return new BasicHttpRequest("GET", command);
                return new BasicHttpEntityEnclosingRequest("POST", command);
            } else {
                // No new request to submit
                return null;
            }
        }
        
        public void handleResponse(final HttpResponse response, final HttpContext context) throws IOException 
        {
            responseOut = response;
            
            context.setAttribute(RESPONSE_RECEIVED, Boolean.TRUE);

            requestCount.countDown();
        }  
    }
    
    static class MySessionRequestCallback implements SessionRequestCallback 
    {
        private final CountDownLatch requestCount;        
        
        public MySessionRequestCallback(final CountDownLatch requestCount) {
            super();
            this.requestCount = requestCount;
        }
        
        public void cancelled(final SessionRequest request) {
            this.requestCount.countDown();
        }

        public void completed(final SessionRequest request) {
        }

        public void failed(final SessionRequest request) {
            this.requestCount.countDown();
        }

        public void timeout(final SessionRequest request) {
            this.requestCount.countDown();
        }
    }
    
    static class EventLogger implements EventListener 
    {
        public void connectionOpen(final NHttpConnection conn) {
            System.out.println("Connection open: " + conn);
        }

        public void connectionTimeout(final NHttpConnection conn) {
            System.out.println("Connection timed out: " + conn);
        }

        public void connectionClosed(final NHttpConnection conn) {
            System.out.println("Connection closed: " + conn);
        }

        public void fatalIOException(final IOException ex, final NHttpConnection conn) {
            System.err.println("I/O error: " + ex.getMessage());
        }

        public void fatalProtocolException(final HttpException ex, final NHttpConnection conn) {
            System.err.println("HTTP error: " + ex.getMessage());
        }   
    }
}