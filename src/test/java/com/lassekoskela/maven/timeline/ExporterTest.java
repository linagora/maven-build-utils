package com.lassekoskela.maven.timeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.lassekoskela.maven.bean.Goal;
import com.lassekoskela.maven.bean.Phase;
import com.lassekoskela.maven.bean.Project;
import com.lassekoskela.maven.bean.Timeline;
import com.lassekoskela.maven.timeline.GoalOrganizer.DisplayableGoal;
import com.lassekoskela.time.Duration;

public class ExporterTest {

	private String filePath;
	private Exporter exporter;

	@Before
	public void setUp() throws FileNotFoundException {
		filePath = "timeline.html";
		File exportFile = new File(filePath);
		if (exportFile.exists() && exportFile.isDirectory()) {
			for (File innerFile : exportFile.listFiles()) {
				innerFile.delete();
			}
			exportFile.delete();
		}
		exporter = new Exporter(new ConsoleLogger());
	}
	
	@Test(expected=NullPointerException.class)
	public void testBuildItemModelOnNullGoal() {
		DisplayableGoal goal = null;
		exporter.buildItemModel(goal);
	}
	
	@Test
	public void testBuildItemModel() {
		DisplayableGoal goal = new DisplayableGoal("project1", "phase1", "goal1", "dep1 dep2", 100, 200, 300);
		
		assertEquals(exporter.buildItemModel(goal),
				ImmutableMap.of(
						"projectId", "project1",
						"phaseId", "phase1",
						"goalId", "goal1",
						"projectDeps", "dep1 dep2",
						"cssStyle", "height:300px;top:200px;left:100px;"));
	}
	
	@Test(expected=NullPointerException.class)
	public void testBuildTimelineModelOnNull() {
		exporter.buildTimelineModel(null);
	}
	
	@Test
	public void testBuildTimelineModel() {
		assertEquals(exporter.buildTimelineModel("serializedTimeline"),
				ImmutableMap.of("timeline", "serializedTimeline"));
	}
	
	@Test
	public void testGetExportFile() {
		File exportFile = exporter.getExportFile(filePath);
		assertFalse(exportFile.exists());
	}

	@Test
	public void testGetExportFileWhenAlreadyExists() throws IOException {
		File file = new File(filePath);
		file.createNewFile();
		assertTrue(file.exists());
		
		File exportFile = exporter.getExportFile(filePath);
		
		assertFalse(exportFile.exists());
	}
	
	@Test(expected=TimelineExportException.class)
	public void testGetExportFileWhenAlreadyExistsAsNonEmptyDirectory() throws Exception {
		File directory = new File(filePath);
		directory.mkdir();
		new File(directory, "child.txt").createNewFile();
		
		exporter.export(new Timeline(ImmutableSet.<Project>of()));
	}
	
	@Test
	public void testExportTimelineTwoProjects() throws Exception {
		Timeline timeline = new Timeline(ImmutableSet.of(
			new Project("project1", ImmutableSet.of(
				new Phase("validate", ImmutableSet.of(
						new Goal("goal0", new Duration(2000), 0, ImmutableSet.<String>of()),
						new Goal("goal1", new Duration(80000), 2800, ImmutableSet.of("project1-validate-goal0")),
						new Goal("goal2", new Duration(40000), 85000, ImmutableSet.of("project1-validate-goal0 project1-validate-goal1")))),
				new Phase("compile", ImmutableSet.of(
						new Goal("goal1", new Duration(12000), 0, ImmutableSet.<String>of()),
						new Goal("goal2", new Duration(100000), 20000, ImmutableSet.of("project1-validate-goal0", "project1-compile-goal1")))))),
			new Project("project2", ImmutableSet.of(
				new Phase("test", ImmutableSet.of(
						new Goal("goal1", new Duration(2000), 83000, ImmutableSet.<String>of()),
						new Goal("goal2", new Duration(8000), 86000, ImmutableSet.of("project2-test-goal1")))),
				new Phase("install", ImmutableSet.of(
						new Goal("goal1", new Duration(12000), 30000, ImmutableSet.of("project2-install-goal2", "project2-test-goal2")),
						new Goal("goal2", new Duration(4000), 28000, ImmutableSet.<String>of())))))));
		
		File htmlFile = exporter.export(timeline);
		
		Document htmlDoc = XMLUnit.buildControlDocument(new InputSource(new FileInputStream(htmlFile)));
		Document expectedHtmlDoc = XMLUnit.buildTestDocument(new InputSource(ClassLoader.getSystemResourceAsStream("timeline_two_projects.html")));
		
		XMLUnit.setIgnoreWhitespace(true);
		XMLAssert.assertXMLEqual(htmlDoc, expectedHtmlDoc);
	}
}
