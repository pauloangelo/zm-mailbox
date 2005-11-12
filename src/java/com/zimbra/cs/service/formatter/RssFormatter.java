/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import net.fortuna.ical4j.model.Parameter;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.util.Constants;
import com.zimbra.soap.Element;

public class RssFormatter extends Formatter {

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        
    public boolean format(Context context, MailItem mailItem) throws IOException, ServiceException {
        
        Iterator iterator = getMailItems(context, mailItem, getDefaultStartTime(), getDefaultEndTime());
        
        context.resp.setContentType("application/rss+xml");
        
        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\"?>");
            
        Element.XMLElement rss = new Element.XMLElement("rss");
        rss.addAttribute("version", "2.0");

        Element channel = rss.addElement("channel");
        channel.addElement("title").setText("Zimbra " + context.itemPath);
            
        channel.addElement("generator").setText("Zimbra RSS Feed Servlet");

                
        //channel.addElement("description").setText(query);
        
//        MailDateFormat mdf = new MailDateFormat();
        while(iterator.hasNext()) {
            MailItem itItem = (MailItem) iterator.next();
            if (itItem instanceof Appointment) {
                addAppointment((Appointment)itItem, channel, context);                
            } else if (itItem instanceof Message) {
                addMessage((Message) itItem, channel, context);
            }
        }
        sb.append(rss.toString());
        context.resp.getOutputStream().write(sb.toString().getBytes());
        return true;
    }

    public long getDefaultStartTime() {    
        return System.currentTimeMillis() - (7*Constants.MILLIS_PER_DAY);
    }

    // eventually get this from query param ?end=long|YYYYMMMDDHHMMSS
    public long getDefaultEndTime() {
        return System.currentTimeMillis() + (7*Constants.MILLIS_PER_DAY);
    }
    
    private void addAppointment(Appointment appt, Element channel, Context context) {
        Collection instances = appt.expandInstances(context.getStartTime(), context.getEndTime());
        for (Iterator instIt = instances.iterator(); instIt.hasNext(); ) {
            Appointment.Instance inst = (Appointment.Instance) instIt.next();
            InviteInfo invId = inst.getInviteInfo();
            Invite inv = appt.getInvite(invId.getMsgId(), invId.getComponentId());
            Element rssItem = channel.addElement("item");
            rssItem.addElement("title").setText(inv.getName());
            rssItem.addElement("pubDate").setText(mDateFormat.format(new Date(inst.getStart())));
            /*                
            StringBuffer desc = new StringBuffer();
            sb.append("Start: ").append(sdf.format(new Date(inst.getStart()))).append("\n");
            sb.append("End: ").append(sdf.format(new Date(inst.getEnd()))).append("\n");
            sb.append("Location: ").append(inv.getLocation()).append("\n");
            sb.append("Notes: ").append(inv.getFragment()).append("\n");
            item.addElement("description").setText(sb.toString());
            */
            rssItem.addElement("description").setText(inv.getFragment());
            rssItem.addElement("author").setText(CalendarUtils.paramVal(inv.getOrganizer(), Parameter.CN));
        }                    
        
    }
     
    private void addMessage(Message m, Element channel, Context context) {
        Element item = channel.addElement("item");
        item.addElement("title").setText(m.getSubject());
        item.addElement("description").setText(m.getFragment());
        item.addElement("author").setText(m.getSender());
        item.addElement("pubDate").setText(mDateFormat.format(new Date(m.getDate())));
        /* TODO: guid, links, etc */
        // Element guid = item.addElement("guid");
        // guid.setText(acct.getId()+"/"+m.getId());
        // guid.addAttribute("isPermaLink", "false");
    }

    public String getType() {
        return "rss";
    }
}
