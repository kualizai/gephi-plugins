/*
Copyright 2008-2010 Gephi
Authors : Mathieu Bastian <mathieu.bastian@gephi.org>, Andre Panisson <panisson@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2011 Gephi Consortium. All rights reserved.

The contents of this file are subject to the terms of either the GNU
General Public License Version 3 only ("GPL") or the Common
Development and Distribution License("CDDL") (collectively, the
"License"). You may not use this file except in compliance with the
License. You can obtain a copy of the License at
http://gephi.org/about/legal/license-notice/
or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
specific language governing permissions and limitations under the
License.  When distributing the software, include this License Header
Notice in each file and include the License files at
/cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
License Header, with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 3, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 3] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 3 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 3 code and therefore, elected the GPL
Version 3 license, then the option applies only if the new code is
made subject to such option by the copyright holder.

Contributor(s):

Portions Copyrighted 2011 Gephi Consortium.
 */
package org.gephi.streaming.server.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.gephi.data.attributes.api.AttributeController;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.streaming.api.GraphUpdaterEventHandler;
import org.gephi.streaming.api.StreamReader;
import org.gephi.streaming.api.StreamReaderFactory;
import org.gephi.streaming.api.event.GraphEventBuilder;
import org.gephi.streaming.server.ServerController;
import org.gephi.streaming.server.impl.simpleframework.RequestWrapper;
import org.gephi.streaming.server.impl.simpleframework.ResponseWrapper;
import org.gephi.streaming.server.impl.ServerControllerImpl;
import org.openide.util.Lookup;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * From linux, you can connect to the server using:<br>
 * wget -qO- http://localhost:8080/graphstream
 * 
 * @author panisson
 *
 */
public class MainServer2 implements Container {
	
	private static final String DGS_RESOURCE = "amazon_0201485419_400.dgs";
	
	private ServerController serverController;
	
	public MainServer2() {
	    
	    ProjectController projectController = Lookup.getDefault().lookup(ProjectController.class);
        projectController.newProject();
        Workspace workspace = projectController.newWorkspace(projectController.getCurrentProject());
        
        AttributeController ac = Lookup.getDefault().lookup(AttributeController.class);
        ac.getModel();
        
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        GraphModel graphModel = graphController.getModel();
	    
        serverController = new ServerControllerImpl(graphModel.getHierarchicalMixedGraph());
	    
	    final InputStream fileInputStream = this.getClass().getResourceAsStream(DGS_RESOURCE);
        
        final InputStream inputStream = new InputStream() {
            private int count = 0;

            @Override
            public int read() throws IOException {
                count++;
                if (count%10 == 0)
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) { }
                return fileInputStream.read();
            }
        };

        StreamReaderFactory factory = Lookup.getDefault().lookup(StreamReaderFactory.class);
        final StreamReader streamReader = factory.createStreamReader("DGS", new GraphUpdaterEventHandler(graphModel.getHierarchicalMixedGraph()), new GraphEventBuilder(this));

        new Thread() {
            @Override
            public void run() {

                try {
                    streamReader.processStream(inputStream);

                } catch (IOException e) {
                    // Exception during processing
                    e.printStackTrace();
                }
            }
        }.start();
	}

   public void handle(Request request, Response response) {
	   long time = System.currentTimeMillis();

	   response.set("Content-Type", "text/plain");
	   response.set("Server", "Gephi/0.7 alpha4");
	   response.setDate("Date", time);
	   response.setDate("Last-Modified", time);
	   
	   try {
		   serverController.handle(new RequestWrapper(request), new ResponseWrapper(response, null));
		   
	   } catch (Exception e) {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
	   }
   }

   public static void main(String[] list) throws Exception {
      Container container = new MainServer2();
      Connection connection = new SocketConnection(container);
      SocketAddress address = new InetSocketAddress(8080);

      connection.connect(address);
      
      
   }
}