package gov.nysenate.openleg.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;

import gov.nysenate.openleg.lucene.LuceneResult;

public class SearchEngine2 extends SearchEngine {

	private static SearchEngine2 _instance = null;
	
	public static void main(String[] args) throws Exception {
		SearchEngine2 engine = SearchEngine2.getInstance();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		String line = null;
		System.out.print("openlegLuceneConsole> ");
		while (!(line = reader.readLine()).equals("quit"))
		{
			if (line.startsWith("optimize"))
				engine.optimize();
			else if (line.startsWith("delete"))
			{
				StringTokenizer cmd = new StringTokenizer(line.substring(line.indexOf(" ")+1)," ");
				String type = cmd.nextToken();
				String id = (cmd.hasMoreTokens() ? cmd.nextToken() : null);
				engine.deleteSenateObjectById(type, id);
			}
			else if (line.startsWith("create"))
				engine.createIndex();
			else {
				SenateResponse sr = engine.search(line, "xml", 1, 10, null, false);
				if(sr != null && !sr.getResults().isEmpty()) {
					for(Result r:sr.getResults()) {
						System.out.println(r.getOid());
					}
				}
			}
			
			System.out.print("openleg search > ");
		}
		System.out.println("Exiting Search Engine");
	}

	private SearchEngine2() {
  		super("/usr/local/openleg/lucene/2");
		logger = Logger.getLogger(SearchEngine2.class);
	}
	
	public static synchronized SearchEngine2 getInstance (){ 
		if (_instance == null) {
			_instance = new SearchEngine2();
		}
		
		return _instance;
	}
	
	public SenateResponse search(String searchText, String format, int start, int max, String sortField, boolean reverseSort) throws ParseException, IOException {
		
    	String data = "o"+format.toLowerCase()+"";
    	
    	LuceneResult result = search(searchText,start,max,sortField,reverseSort);
    	
    	SenateResponse response = new SenateResponse();
    	
    	if (result == null)
    	{
    		response.addMetadataByKey("totalresults", 0 );
    	}
    	else
    	{
	    	response.addMetadataByKey("totalresults", result.total );
	    	
	    	for (Document doc : result.results) {
	    		String lastModified = doc.get("modified");
	    		if (lastModified == null || lastModified.length() == 0)
	    			lastModified = new Date().getTime()+"";
	    		
	    		response.addResult(new Result(
	    				doc.get("otype"),
	    				doc.get(data),
	    				doc.get("oid"),
	    				Long.parseLong(lastModified),
	    				Boolean.parseBoolean(doc.get("active"))));
	    	}
	    	
    	}
	    	
    	return response;
	}
}
