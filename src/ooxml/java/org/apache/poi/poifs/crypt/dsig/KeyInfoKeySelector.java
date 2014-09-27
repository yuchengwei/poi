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

/* ====================================================================
   This product contains an ASLv2 licensed version of the OOXML signer
   package from the eID Applet project
   http://code.google.com/p/eid-applet/source/browse/trunk/README.txt  
   Copyright (C) 2008-2014 FedICT.
   ================================================================= */ 

package org.apache.poi.poifs.crypt.dsig;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

/**
 * JSR105 key selector implementation using the ds:KeyInfo data of the signature
 * itself.
 */
public class KeyInfoKeySelector extends KeySelector implements KeySelectorResult {

    private static final POILogger LOG = POILogFactory.getLogger(KeyInfoKeySelector.class);

    private X509Certificate certificate;

    @SuppressWarnings("unchecked")
    @Override
    public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method, XMLCryptoContext context) throws KeySelectorException {
        LOG.log(POILogger.DEBUG, "select key");
        if (null == keyInfo) {
            throw new KeySelectorException("no ds:KeyInfo present");
        }
        List<XMLStructure> keyInfoContent = keyInfo.getContent();
        this.certificate = null;
        for (XMLStructure keyInfoStructure : keyInfoContent) {
            if (false == (keyInfoStructure instanceof X509Data)) {
                continue;
            }
            X509Data x509Data = (X509Data) keyInfoStructure;
            List<Object> x509DataList = x509Data.getContent();
            for (Object x509DataObject : x509DataList) {
                if (false == (x509DataObject instanceof X509Certificate)) {
                    continue;
                }
                X509Certificate certificate = (X509Certificate) x509DataObject;
                LOG.log(POILogger.DEBUG, "certificate", certificate.getSubjectX500Principal());
                if (null == this.certificate) {
                    /*
                     * The first certificate is presumably the signer.
                     */
                    this.certificate = certificate;
                }
            }
            if (null != this.certificate) {
                return this;
            }
        }
        throw new KeySelectorException("No key found!");
    }

    public Key getKey() {
        return this.certificate.getPublicKey();
    }

    /**
     * Gives back the X509 certificate used during the last signature
     * verification operation.
     * 
     * @return
     */
    public X509Certificate getCertificate() {
        return this.certificate;
    }
}