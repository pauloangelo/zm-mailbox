/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.EnumSet;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * Unit test for {@link ZimbraQuery}.
 *
 * @author ysasaki
 */
public final class ZimbraQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void checkSortCompatibility() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        SearchParams params = new SearchParams();
        params.setQueryStr("in:inbox content:test");

        params.setSortBy(SortBy.RCPT_ASC);
        try {
            new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
            Assert.fail();
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }

        params.setSortBy(SortBy.ATTACHMENT_ASC);
        try {
            new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
            Assert.fail();
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }

        params.setSortBy(SortBy.FLAG_ASC);
        try {
            new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
            Assert.fail();
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }

        params.setSortBy(SortBy.PRIORITY_ASC);
        try {
            new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
            Assert.fail();
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void notClause() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, new ParsedMessage("From: test1@zimbra.com".getBytes(), false), dopt);
        Message msg2 = mbox.addMessage(null, new ParsedMessage("From: test2@zimbra.com".getBytes(), false), dopt);
        Message msg3 = mbox.addMessage(null, new ParsedMessage("From: test3@zimbra.com".getBytes(), false), dopt);
        mbox.index.indexDeferredItems();

        SearchParams params = new SearchParams();
        params.setQueryStr("-from:test1@zimbra.com");
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setSortBy(SortBy.NONE);
        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        ZimbraQueryResults results = query.execute();
        Assert.assertTrue(results.hasNext());
        Assert.assertEquals(msg2.getId(), results.getNext().getItemId());
        Assert.assertTrue(results.hasNext());
        Assert.assertEquals(msg3.getId(), results.getNext().getItemId());
        Assert.assertFalse(results.hasNext());
        results.doneWithSearchResults();
    }

}
