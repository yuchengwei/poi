/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hpsf;

import java.io.UnsupportedEncodingException;

import org.apache.poi.util.Removal;

/**
 * <p>Adds writing capability to the {@link Section} class.</p>
 *
 * <p>Please be aware that this class' functionality will be merged into the
 * {@link Section} class at a later time, so the API will change.</p>
 *
 * @deprecated POI 3.16 - use Section as base class instead
 */
@Removal(version="3.18")
public class MutableSection extends Section {
    public MutableSection() {}

    public MutableSection(final Section s) {
    	super(s);
    }
    
    public MutableSection(final byte[] src, final int offset) throws UnsupportedEncodingException {
        super(src,offset);
    }
}
