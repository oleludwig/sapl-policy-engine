/**
 * Copyright © 2020 Dominic Heutelbeck (dominic@heutelbeck.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.pdp.embedded.config.resources;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.sapl.api.pdp.PolicyDecisionPointConfiguration;
import io.sapl.interpreter.combinators.DocumentsCombinator;
import io.sapl.pdp.embedded.config.PDPConfigurationProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class ResourcesPDPConfigurationProvider implements PDPConfigurationProvider {

	private static final String DEFAULT_CONFIG_PATH = "/policies";
	private static final String CONFIG_FILE_GLOB_PATTERN = "pdp.json";

	private final ObjectMapper mapper;
	private final PolicyDecisionPointConfiguration config;

	public ResourcesPDPConfigurationProvider() {
		this(DEFAULT_CONFIG_PATH);
	}

	public ResourcesPDPConfigurationProvider(String configPath) {
		this(configPath, new ObjectMapper());
	}

	public ResourcesPDPConfigurationProvider(@NonNull String configPath, @NonNull ObjectMapper mapper) {
		this(ResourcesPDPConfigurationProvider.class, configPath, mapper);
	}

	public ResourcesPDPConfigurationProvider(@NonNull Class<?> clazz, @NonNull String configPath,
			@NonNull ObjectMapper mapper) {
		log.info("Loading the PDP configuration from bundled resources: '{}'", configPath);
		this.mapper = mapper;
		URL configFolderUrl = clazz.getResource(configPath);
		if (configFolderUrl == null) {
			throw new RuntimeException("Config folder not found. Path:" + configPath + " - URL: null");
		}

		if ("jar".equals(configFolderUrl.getProtocol())) {
			config = readConfigFromJar(configFolderUrl);
		} else {
			config = readConfigFromDirectory(configFolderUrl);
		}
	}

	private PolicyDecisionPointConfiguration readConfigFromJar(URL configFolderUrl) {
		log.debug("reading config from jar {}", configFolderUrl);
		final String[] jarPathElements = configFolderUrl.toString().split("!");
		final String jarFilePath = jarPathElements[0].substring("jar:file:".length());
		final StringBuilder dirPath = new StringBuilder();
		for (int i = 1; i < jarPathElements.length; i++) {
			dirPath.append(jarPathElements[i]);
		}
		if (dirPath.charAt(0) == File.separatorChar) {
			dirPath.deleteCharAt(0);
		}
		final String configFilePath = dirPath.append(File.separatorChar).append(CONFIG_FILE_GLOB_PATTERN).toString();

		try (ZipFile zipFile = new ZipFile(jarFilePath)) {
			Enumeration<? extends ZipEntry> e = zipFile.entries();

			while (e.hasMoreElements()) {
				ZipEntry entry = e.nextElement();
				if (!entry.isDirectory() && entry.getName().equals(configFilePath)) {
					log.info("loading PDP configuration: {}", entry.getName());
					BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
					String fileContentsStr = IOUtils.toString(bis, StandardCharsets.UTF_8);
					bis.close();
					return mapper.readValue(fileContentsStr, PolicyDecisionPointConfiguration.class);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error loading PDP configuration from resources: " + configFolderUrl);
		}
		log.info("No PDP configuration found in resources. Using defaults.");
		return new PolicyDecisionPointConfiguration();
	}

	private PolicyDecisionPointConfiguration readConfigFromDirectory(URL configFolderUrl) {
		log.debug("reading config from directory {}", configFolderUrl);
		Path configDirectoryPath;
		try {
			configDirectoryPath = Paths.get(configFolderUrl.toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error accessing PDP configuration: " + e.getMessage());
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDirectoryPath, CONFIG_FILE_GLOB_PATTERN)) {
			for (Path filePath : stream) {
				log.info("loading PDP configuration: {}", filePath.toAbsolutePath());
				return mapper.readValue(filePath.toFile(), PolicyDecisionPointConfiguration.class);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error accessing PDP configuration: " + e.getMessage());
		}
		log.info("No PDP configuration found in resources. Using defaults.");
		return new PolicyDecisionPointConfiguration();
	}

	@Override
	public Flux<DocumentsCombinator> getDocumentsCombinator() {
		return Flux.just(config.getAlgorithm()).map(this::convert);
	}

	@Override
	public Flux<Map<String, JsonNode>> getVariables() {
		return Flux.just(config.getVariables()).map(HashMap::new);
	}

	@Override
	public void dispose() {
		// NOP nothing to dispose
	}
}
