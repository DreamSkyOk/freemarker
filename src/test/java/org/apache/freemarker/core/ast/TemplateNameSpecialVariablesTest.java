/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.freemarker.core.ast;

import java.io.IOException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Before;
import org.junit.Test;

public class TemplateNameSpecialVariablesTest extends TemplateTest {
    
    private static TemplateLoader createTemplateLoader(String specVar) {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("main.ftl",
                "In main: ${" + specVar + "}\n"
                + "<#import 'imp.ftl' as i>"
                + "In imp: ${inImp}\n"
                + "In main: ${" + specVar + "}\n"
                + "<@i.impM>${" + specVar + "}</@>\n"
                + "<@i.impM2 />\n"
                + "In main: ${" + specVar + "}\n"
                + "<#include 'inc.ftl'>"
                + "In main: ${" + specVar + "}\n"
                + "<@incM>${" + specVar + "}</@>\n"
                + "<@incM2 />\n"
                + "In main: ${" + specVar + "}\n"
                );
        tl.putTemplate("imp.ftl",
                "<#global inImp = " + specVar + ">"
                + "<#macro impM>"
                    + "${" + specVar + "}\n"
                    + "{<#nested>}"
                + "</#macro>"
                + "<#macro impM2>"
                    + "In imp call imp:\n"
                    + "<@impM>${" + specVar + "}</@>\n"
                    + "After: ${" + specVar + "}"
                + "</#macro>"
                );
        tl.putTemplate("inc.ftl",
                "In inc: ${" + specVar + "}\n"
                + "In inc call imp:\n"
                + "<@i.impM>${" + specVar + "}</@>\n"
                + "<#macro incM>"
                    + "${" + specVar + "}\n"
                    + "{<#nested>}"
                + "</#macro>"
                + "<#macro incM2>"
                    + "In inc call imp:\n"
                    + "<@i.impM>${" + specVar + "}</@>"
                + "</#macro>"
                );
        return tl;
    }

    private static final String PRINT_ALL_FTL
            = "t=${.templateName}, ct=${.currentTemplateName!'-'}, mt=${.mainTemplateName!'-'}";
    
    @Test
    public void testTemplateName() throws IOException, TemplateException {
        getConfiguration().setTemplateLoader(createTemplateLoader(".templateName"));
        assertMainFtlOutput(false);
    }

    @Test
    public void testMainTemplateName() throws IOException, TemplateException {
        getConfiguration().setTemplateLoader(createTemplateLoader(".mainTemplateName"));
        assertMainFtlOutput(true);
    }

    private void assertMainFtlOutput(boolean allMain) throws IOException, TemplateException {
        String expected
                = "In main: main.ftl\n"
                + "In imp: imp.ftl\n"
                + "In main: main.ftl\n"
                + "main.ftl\n"
                + "{main.ftl}\n"
                + "In imp call imp:\n"
                + "main.ftl\n"
                + "{imp.ftl}\n"
                + "After: main.ftl\n"
                + "In main: main.ftl\n"
                + "In inc: inc.ftl\n"
                + "In inc call imp:\n"
                + "inc.ftl\n"
                + "{main.ftl}\n"
                + "In main: main.ftl\n"
                + "main.ftl\n"
                + "{main.ftl}\n"
                + "In inc call imp:\n"
                + "main.ftl\n"
                + "{main.ftl}\n"
                + "In main: main.ftl\n";
        if (allMain) {
            expected = _StringUtil.replace(expected, "imp.ftl", "main.ftl");
            expected = _StringUtil.replace(expected, "inc.ftl", "main.ftl");
        }
        assertOutputForNamed("main.ftl", expected);
    }

    @Test
    public void testCurrentTemplateName() throws IOException, TemplateException {
        getConfiguration().setTemplateLoader(createTemplateLoader(".currentTemplateName"));
        assertOutputForNamed("main.ftl",
                "In main: main.ftl\n"
                + "In imp: imp.ftl\n"
                + "In main: main.ftl\n"
                + "imp.ftl\n"
                + "{main.ftl}\n"
                + "In imp call imp:\n"
                + "imp.ftl\n"
                + "{imp.ftl}\n"
                + "After: imp.ftl\n"
                + "In main: main.ftl\n"
                + "In inc: inc.ftl\n"
                + "In inc call imp:\n"
                + "imp.ftl\n"
                + "{inc.ftl}\n"
                + "In main: main.ftl\n"
                + "inc.ftl\n"
                + "{main.ftl}\n"
                + "In inc call imp:\n"
                + "imp.ftl\n"
                + "{inc.ftl}\n"
                + "In main: main.ftl\n");
    }

    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setWhitespaceStripping(false);
    }
    
    @Test
    public void testInAdhocTemplate() throws TemplateException, IOException {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("inc.ftl", "Inc: " + PRINT_ALL_FTL);
        getConfiguration().setTemplateLoader(tl);
        
        // In nameless templates, the deprecated .templateName is "", but the new variables are missing values. 
        assertOutput(new Template(null, PRINT_ALL_FTL + "; <#include 'inc.ftl'>", getConfiguration()),
                "t=, ct=-, mt=-; Inc: t=inc.ftl, ct=inc.ftl, mt=-");
        
        assertOutput(new Template("foo.ftl", PRINT_ALL_FTL + "; <#include 'inc.ftl'>", getConfiguration()),
                "t=foo.ftl, ct=foo.ftl, mt=foo.ftl; Inc: t=inc.ftl, ct=inc.ftl, mt=foo.ftl");
    }

    @Test
    public void testInInterpretTemplate() throws TemplateException, IOException {
        getConfiguration().setSharedVariable("t", PRINT_ALL_FTL);
        assertOutput(new Template("foo.ftl", PRINT_ALL_FTL + "; <@t?interpret />", getConfiguration()),
                "t=foo.ftl, ct=foo.ftl, mt=foo.ftl; "
                + "t=foo.ftl->anonymous_interpreted, ct=foo.ftl->anonymous_interpreted, mt=foo.ftl");
        assertOutput(new Template(null, PRINT_ALL_FTL + "; <@t?interpret />", getConfiguration()),
                "t=, ct=-, mt=-; "
                + "t=nameless_template->anonymous_interpreted, ct=nameless_template->anonymous_interpreted, mt=-");
        assertOutput(new Template("foo.ftl", PRINT_ALL_FTL + "; <@[t,'bar']?interpret />", getConfiguration()),
                "t=foo.ftl, ct=foo.ftl, mt=foo.ftl; "
                + "t=foo.ftl->bar, ct=foo.ftl->bar, mt=foo.ftl");
    }
    
}