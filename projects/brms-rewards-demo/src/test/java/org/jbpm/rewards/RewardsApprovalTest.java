package org.jbpm.rewards;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;
import org.drools.builder.ResourceType;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler;
import org.jbpm.task.AccessType;
import org.jbpm.task.Status;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.ContentData;
import org.jbpm.task.TaskService;
import org.jbpm.task.service.responsehandlers.BlockingTaskSummaryResponseHandler;
import org.jbpm.test.JbpmJUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;



/**
* This is a sample file to unit test the Extended Rewards Approval process.
*/

public class RewardsApprovalTest extends JbpmJUnitTestCase {

	private static StatefulKnowledgeSession ksession;
	private static TaskService taskService;
	private static Map<String, Object> params;
	private static ProcessInstance processInstance;

	public RewardsApprovalTest() {

		super(true);

	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		// nothing yet.
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		

		// load up the knowledge base
		KnowledgeBase kbase = null;
	
		// Use the local files.
	    final Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
	    resources.put("rewardsapproval.bpmn2", ResourceType.BPMN2);

	    try {
	    	kbase = createKnowledgeBase(resources);
	 	} catch (Exception e) {
	 		e.printStackTrace();
	 	}

	    ksession = createKnowledgeSession(kbase);
		taskService = getTaskService(ksession);

		// register work items.
		ksession.getWorkItemManager().registerWorkItemHandler("Log", new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email", new SystemOutWorkItemHandler());

		params = new HashMap<String, Object>();
		// initialize process parameters.
		params.put("employee", "erics");
		params.put("reason", "Amazing demos for JBoss World!");		
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	@Test
	public void rewardApprovedTest() {

		// start process.
		processInstance = ksession.startProcess("org.jbpm.approval.rewards", params);

		// execute task by Mary from HR.		
		List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("mary", new ArrayList<String>(), "en-UK");  // NP error here.
		TaskSummary task = list.get(0);
		taskService.claim(task.getId(), "mary", new ArrayList<String>());
		taskService.start(task.getId(), "mary");
		
		Map<String, Object> taskParams = new HashMap<String, Object>();
		taskParams.put("explanation", "Great work");
		taskParams.put("outcome", "Approved");
		
		// Serialized and inserted.
		ContentData content = new ContentData();
		content.setAccessType(AccessType.Inline);
		content.setContent(getByteArrayFromObject(taskParams));
		
		// add results of task.
		taskService.complete(task.getId(), "mary", content);

		// test for completion and in correct end node.
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		assertNodeTriggered(processInstance.getId(), "End Approved");
	}

	/**
	 * Converts an object to a serialized byte array.
	 * 
	 * @param obj Object to be converted.
	 * @return byte[] Serialized array representing the object.
	 */
	public static byte[] getByteArrayFromObject(Object obj) {
	    byte[] result = null;
	       
	    try {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        ObjectOutputStream oos = new ObjectOutputStream(baos);
	        oos.writeObject(obj);
	        oos.flush();
	        oos.close();
	        baos.close();
	        result = baos.toByteArray();
	    } catch (IOException ioEx) {
	        Logger.getLogger("UtilityMethods").error("Error converting object to byteArray", ioEx);
	    }
	        
	    return result;
	}

}

