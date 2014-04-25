/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.reporting.report.renderer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Set;

import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.module.reporting.common.Localized;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.definition.ReportDefinition;

/**
 * A Renderer that will display a simple dojo graph using the definitions
 */
@Handler
@Localized("reporting.SimpleDojoReportRenderer")
public class SimpleDojoReportRenderer extends ReportDesignRenderer 
{
	/**
     * @see ReportRenderer#getRenderedContentType(ReportDefinition, String)
     */
    public String getRenderedContentType(ReportDefinition schema, String argument) {
    	return "text/html";
    }

	/**
	 * @see ReportRenderer#getFilename(ReportDefinition)
	 */
	public String getFilename(ReportDefinition schema, String argument) {
		return schema.getName() + ".html";
	}

	/**
	 * @see ReportRenderer#render(ReportData, String, OutputStream)
	 */
	public void render(ReportData results, String argument, OutputStream out) throws IOException, RenderingException
	{
		HashMap<String, Integer> valuesMap = new HashMap<String, Integer>();
		String title = "";
		
		for (String key: results.getDataSets().keySet())
		{
			title = key;
			DataSet dataset = results.getDataSets().get(key);
			List<DataSetColumn> columns = dataset.getMetaData().getColumns();
		
			for (DataSetRow row: dataset)
			{
				for (DataSetColumn column: columns)
				{
					Object colValue = row.getColumnValue(column.getName());
					if (colValue != null)
					{
						if (colValue instanceof Cohort)
						{
							//do nothing
						}
						else
						{
							if (valuesMap.containsKey(colValue.toString()))
							{
								Integer currVal = valuesMap.get(colValue.toString());
								valuesMap.put(colValue.toString(), currVal + 1);
							}
							else
							{
								valuesMap.put(colValue.toString(), 1);
							}
						}
					}
				}
			}
		}

		Set<String> keys = valuesMap.keySet();
		ArrayList<String> keysList = new ArrayList<String>(keys);
		Collections.sort(keysList);
		StringBuilder dataString = new StringBuilder("var chartData = [");
		StringBuilder labelsString = new StringBuilder("labels: [");
		int count = 1;
		for (String key: keysList)
		{
			Integer val = valuesMap.get(key);
			dataString.append(val + ",");
			labelsString.append("{value:" + count + ", text: \"" + key + "\"},");
			count++;
		}
		dataString.deleteCharAt(dataString.length() - 1);
		labelsString.deleteCharAt(labelsString.length() - 1);

		dataString.append("];");
		labelsString.append("],");
		
		int maxBarWidth = 40;

		Writer w = new OutputStreamWriter(out,"UTF-8");
		
		w.write("<html>\n");
		w.write("<head>\n");
		w.write("<script src=\"http://ajax.googleapis.com/ajax/libs/dojo/1.9.2/dojo/dojo.js\">\n");
		w.write("</script>\n");
		w.write("<script>\n");
		w.write("dojo.require(\"dojox.charting.Chart2D\");\n");
		w.write("dojo.require(\"dojox.charting.themes.MiamiNice\");\n");

		w.write(dataString.toString() + "\n");
		w.write("dojo.ready(function(){\n");
		w.write("var chart = new dojox.charting.Chart2D(\"chartNode\");\n");
		w.write("var mxBarWidth = " + maxBarWidth + ";");
		w.write("chart.setTheme(dojox.charting.themes.MiamiNice);\nchart.addPlot(\"default\", { type: \"Columns\", markers: true, gap: 5, maxBarSize: mxBarWidth});\n");
		w.write("chart.addAxis(\"x\", {\n");
		w.write(labelsString.toString() + "\n");
		w.write("maxLabelSize: mxBarWidth, trailingSymbol: \"...\"});");
		w.write("chart.addAxis(\"y\", {vertical: true, fixLower: \"major\", fixUpper: \"major\", includeZero: true});\n");
		w.write("chart.addSeries(\"OpenMRS Report\", chartData);\n");
		w.write("chart.render();});\n");
		w.write("</script>\n");
		w.write("</head>\n");
		w.write("<body>\n");
		w.write("<h2>" + title + " Chart</h2>\n");
		w.write("<div id=\"chartNode\" style=\"width: " + (int)(maxBarWidth * keys.size() * 2.5) + "; height: 500;\"></div>\n");
		w.write("</body>\n");
		w.write("</html>\n");

		w.flush();
	}
}