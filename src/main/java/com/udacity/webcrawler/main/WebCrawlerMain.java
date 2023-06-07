package com.udacity.webcrawler.main;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.inject.Inject;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

public final class WebCrawlerMain {

	private final CrawlerConfiguration config;

	private WebCrawlerMain(CrawlerConfiguration config) {
		this.config = Objects.requireNonNull(config);
	}

	@Inject
	private WebCrawler crawler;

	@Inject
	private Profiler profiler;

	private void run() throws Exception {
		Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

		CrawlResult result = crawler.crawl(config.getStartPages());
		CrawlResultWriter resultWriter = new CrawlResultWriter(result);
		String resultPath = config.getResultPath();
		
		if (!resultPath.isEmpty()) {
			Path path = Paths.get(resultPath);
			resultWriter.write(path);
		}
//		else {
//			try (Writer outputWriter = new OutputStreamWriter(System.out)) {
//				resultWriter.write(outputWriter);
//			} catch (IOException ex) {
//				ex.printStackTrace();
//			}
//		}		

		if (!config.getProfileOutputPath().isEmpty()) {
			try (Writer outputWriter = new OutputStreamWriter(System.out)) {
				resultWriter.write(outputWriter);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} else {
			try (Writer outputWriter = new OutputStreamWriter(System.out)) {
				profiler.writeData(outputWriter);
				outputWriter.flush();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: WebCrawlerMain [starting-url]");
			return;
		}

		CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
		new WebCrawlerMain(config).run();
	}
}
