package com.basho.riak.client;

import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.bucket.FetchBucket;
import com.basho.riak.client.bucket.WriteBucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Retrier;
import com.basho.riak.client.operations.RiakOperation;
import com.basho.riak.client.query.*;
import com.basho.riak.client.raw.RawClient;
import com.basho.riak.client.raw.Transport;
import com.basho.riak.client.raw.http.HTTPClientAdapter;
import com.basho.riak.client.raw.pbc.PBClientAdapter;
import com.basho.riak.client.raw.query.indexes.IndexQuery;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * The default implementation of IRiakClient.
 *
 * Provides convenient, transport agnostic ways to perform
 * bucket and query operations on Riak.
 * 
 * <p>
 * This class is a wrapper around a {@link RawClient} of your choice. The {@link RawClient} wrapped is passed to all
 * {@link RiakOperation}s created by this class, so it really needs to be Thread Safe and reusable.
 * <br/>
 * This class provides a {@link Retrier} to each {@link RiakOperation} it creates. If you provide one please make sure it is
 * Thread safe and reusable.
 * <br/>
 * If you do not provide a {@link Retrier} a {@link DefaultRetrier} configured for 3 attempts is created.
 * </p>
 *
 * @author russell
 * 
 * @see RawClient
 * @see PBClientAdapter
 * @see HTTPClientAdapter
 * @see DefaultRetrier
 */
public final class DefaultRiakClient implements IRiakClient {

    private final RawClient rawClient;
    private final Retrier retrier;

    /**
     * Create an instance that wraps the provided {@link RawClient} and passes it and the {@link Retrier}
     * to created operations.
     * 
     * @param rawClient the {@link RawClient} to wrap.
     * @param defaultRetrier the {@link Retrier} that will be set as the default on all {@link RiakOperation}s created by this instance.
     */
    DefaultRiakClient(final RawClient rawClient, final Retrier defaultRetrier) {
        this.rawClient = rawClient;
        this.retrier = defaultRetrier;
    }

    /**
     * Create an instance that wraps the provided {@link RawClient}. A {@link DefaultRetrier} configured for 3 attempts is also created.
     * @param rawClient the {@link RawClient} to wrap.
     */
    DefaultRiakClient(final RawClient rawClient) {
        this(rawClient, DefaultRetrier.attempts(3));
    }

    // BUCKET OPS

    /*
     * (non-Javadoc)
     * 
     * @see com.basho.riak.client.IRiakClient#listBuckets()
     */
    public Set<String> listBuckets() throws RiakException {
        try {
            return rawClient.listBuckets();
        } catch (IOException e) {
            throw new RiakException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.basho.riak.client.IRiakClient#listBucketsStreaming()
     */
    public StreamingOperation<String> listBucketsStreaming() throws RiakException {
        try {
            return rawClient.listBucketsStreaming();
        } catch (IOException ex) {
            throw new RiakException(ex);
        }
    }
    
    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#updateBucket(com.basho.riak.client.bucket.Bucket)
     */
    public WriteBucket updateBucket(final Bucket b) {
        return new WriteBucket(rawClient, b, retrier);
    }

    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#fetchBucket(java.lang.String)
     */
    public FetchBucket fetchBucket(String bucketName) {
        return new FetchBucket(rawClient, bucketName, retrier);
    }

    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#createBucket(java.lang.String)
     */
    public WriteBucket createBucket(String bucketName) {
        return new WriteBucket(rawClient, bucketName, retrier);
    }

    /**
     * (non-Javadoc)
     * @see RawClient#resetBucketProperties(java.lang.String) 
     * 
     */
    public void resetBucket(String bucketName) throws RiakException {
        try {
            rawClient.resetBucketProperties(bucketName);
        } catch (IOException ex) {
            throw new RiakException(ex);
        }
    }
    // CLIENT ID

    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#setClientId(byte[])
     */
    public IRiakClient setClientId(final byte[] clientId) throws RiakException {
        if (clientId == null || clientId.length != 4) {
            throw new IllegalArgumentException("Client Id must be 4 bytes long");
        }
        retrier.attempt((Callable<Void>) () -> {
            rawClient.setClientId(clientId);
            return null;
        });

        return this;
    }

    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#generateAndSetClientId()
     */
    public byte[] generateAndSetClientId() throws RiakException {
        final byte[] clientId = retrier.attempt(rawClient::generateAndSetClientId);

        return clientId;
    }

    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#getClientId()
     */
    public byte[] getClientId() throws RiakException {
        final byte[] clientId = retrier.attempt(rawClient::getClientId);

        return clientId;
    }

    // QUERY

    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#mapReduce()
     */
    public BucketKeyMapReduce mapReduce() {
        return new BucketKeyMapReduce(rawClient);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.basho.riak.newapi.RiakClient#mapReduce(java.lang.String)
     */
    public BucketMapReduce mapReduce(String bucket) {
        return new BucketMapReduce(rawClient, bucket);
    }

    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#mapReduce(java.lang.String, java.lang.String)
     */
    public SearchMapReduce mapReduce(String bucket, String query) {
        return new SearchMapReduce(rawClient, bucket, query);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.basho.riak.client.IRiakClient#mapReduce(com.basho.riak.client.raw
     * .query.indexes.IndexQuery)
     */
    public IndexMapReduce mapReduce(IndexQuery query) {
        return new IndexMapReduce(rawClient, query);
    }

    /*
     * (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#walk(com.basho.riak.client.IRiakObject)
     */
    public LinkWalk walk(IRiakObject startObject) {
        return new LinkWalk(rawClient, startObject);
    }

    // MISC OPS

    /*
     * (non-Javadoc)
     * 
     * @see com.basho.riak.client.IRiakClient#ping()
     */
    public void ping() throws RiakException {
        try {
            rawClient.ping();
        } catch (IOException e) {
            throw new RiakException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#getTransport()
     */
    public Transport getTransport() {
        return rawClient.getTransport();
    }

    public void shutdown(){
        rawClient.shutdown();
    }
    
    /* (non-Javadoc)
     * @see com.basho.riak.client.IRiakClient#stats()
     */
    public Iterable<NodeStats> stats() throws RiakException {
        try {
            return rawClient.stats();
        } catch (Exception e) {
            throw new RiakException(e);
        }
    }
}