/*
 * Copyright 2013 Basho Technologies Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.basho.riak.client.raw.pbc;

import com.basho.riak.client.query.RiakStreamingRuntimeException;
import static com.basho.riak.client.raw.pbc.ConversionUtil.nullSafeToStringUtf8;
import com.basho.riak.pbc.RiakStreamClient;
import com.google.protobuf.ByteString;
import java.io.IOException;


/**
 *
 * @author Brian Roach <roach at basho dot com>
 */
public class PBStreamingList extends PBStreamingOperation<ByteString, String>
{

    public PBStreamingList(RiakStreamClient<ByteString> client)
    {
        super(client);
    }
    
    @Override
    public String next()
    {
        try
        {
            return nullSafeToStringUtf8(client.next());
        }
        catch (IOException ex)
        {
            throw new RiakStreamingRuntimeException(ex);
        }
    }
    
}
