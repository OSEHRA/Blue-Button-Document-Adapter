package org.osehra.das.repo.bluebutton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

import org.osehra.das.C32Document;
import org.osehra.das.wrapper.nwhin.C32DocumentEntity;
import org.osehra.das.wrapper.nwhin.WrapperResource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

public class RepositoryImpl extends AbstractAsyncMsgFormatAware implements Repository {
	protected final String completeStatusString = "COMPLETE";
	protected final String incompleteStatusString = "INCOMPLETE";
	protected JmsTemplate jmsTemplate;
	protected Queue queue;

	public void setConnectionFactory(ConnectionFactory cf) {
        this.jmsTemplate = new JmsTemplate(cf);
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }
    
	public String getIncompleteStatusString() {
		return incompleteStatusString;
	}

	public String getCompleteStatusString() {
		return completeStatusString;
	}

	@Override
	public List<DocStatus> getStatus(String patientId) {
		List<C32DocumentEntity> docList = getAllStoredDocuments(patientId);
		Date today = new Date();
		if (getDocumentByDate(today, docList)==null) {
			sendMessageToRetrieve(today, patientId);
			return getDocStatusNotThere(patientId, today);
		}
		return buildStatusList(docList);
	}

	@Override
	public C32Document getDocument(Date docDate, String patientId) {
		List<C32DocumentEntity> docList = getAllStoredDocuments(patientId); 
		C32DocumentEntity doc = getDocumentByDate(docDate, docList);
		if (doc==null) {
			throw new RuntimeException("document for " + docDate + " " + patientId + " does not exist");
		}
		return getC32Doc(doc);
	}

	protected C32Document getC32Doc(C32DocumentEntity doc) {
		C32Document newDoc = new C32Document();
		newDoc.setDocument(doc.getDocument());
		newDoc.setPatientId(doc.getIcn());
		return newDoc;
	}

	protected C32DocumentEntity getDocumentByDate(Date today, List<C32DocumentEntity> docList) {
		if (docList!=null && !docList.isEmpty()) {
			for (int i=0;i<docList.size();i++) {
				if (today.equals(docList.get(i).getCreateDate())) {
					return docList.get(i);
				}
			}
		}
		return null;
	}

	protected List<C32DocumentEntity> getAllStoredDocuments(String patientId) {
		WrapperResource wr = new WrapperResource();
		return wr.getAllDocuments(patientId);
	}

	protected List<DocStatus> buildStatusList(List<C32DocumentEntity> docList) {
		if (docList==null || docList.isEmpty()) {
			return null;
		}
		List<DocStatus> statusList = new ArrayList<DocStatus>(docList.size());
		for (int i=0;i<docList.size();i++) {
			statusList.add(new DocStatus());
			statusList.get(i).setDateGenerated(docList.get(i).getCreateDate());
			statusList.get(i).setPatientId(docList.get(i).getIcn());
			statusList.get(i).setStatus(getCompleteStatusString());
		}
		return statusList;
	}

	protected List<DocStatus> getDocStatusNotThere(String ptId, Date today) {
		List<DocStatus> docList = new ArrayList<DocStatus>(1);
		docList.add(new DocStatus());
		docList.get(0).setDateGenerated(today);
		docList.get(0).setPatientId(ptId);
		docList.get(0).setStatus(getIncompleteStatusString());
		return docList;
	}

	protected void sendMessageToRetrieve(final Date today, final String patientId) {
		this.jmsTemplate.send(this.queue, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(getAsyncMessageFormat().format(new AsyncRetrieveMessage(today, patientId)));
			}
		});
	}

}