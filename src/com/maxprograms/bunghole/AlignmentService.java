/*******************************************************************************
 * Copyright (c) 2008 - 2025 Håvard Nørjordet.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Håvard Nørjordet - initial API and implementation
 *******************************************************************************/

package com.maxprograms.bunghole;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Convert;
import com.maxprograms.converters.EncodingResolver;
import com.maxprograms.converters.FileFormats;
import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;
import com.maxprograms.bunghole.models.Alignment;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

// NEW: AI-enhanced alignment imports
import com.maxprograms.bunghole.alignment.AlignmentEngine;
import com.maxprograms.bunghole.alignment.AlignmentPair;
import com.maxprograms.bunghole.alignment.AlignmentResult;
import com.maxprograms.bunghole.ai.ClaudeAIService;
import com.maxprograms.bunghole.ai.CostEstimator;
import com.maxprograms.bunghole.ai.CostEstimator.CostEstimate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

public class AlignmentService {

	private static Logger logger = System.getLogger(AlignmentService.class.getName());
	protected boolean aligning;
	protected String alignError;
	protected String status;

	protected boolean loading;
	protected String loadError;
	protected Alignment alignment;

	protected boolean saving;
	protected String saveError;

	// NEW: AI-enhanced alignment components
	private AlignmentEngine alignmentEngine;
	private ClaudeAIService claudeAI;
	private AlignmentResult currentAlignmentResult;

	public AlignmentService() {
		loading = false;
		saving = false;
		aligning = false;

		// Initialize alignment engine with app path
		String appPath = System.getProperty("user.dir");
		this.alignmentEngine = new AlignmentEngine(appPath);

		// Initialize Claude AI if API key is available
		String apiKey = System.getenv("ANTHROPIC_API_KEY");
		if (apiKey != null && !apiKey.isEmpty()) {
			this.claudeAI = new ClaudeAIService(apiKey);
			logger.log(Level.INFO, "Claude AI service initialized");
		} else {
			logger.log(Level.INFO, "Claude AI not available (no API key)");
		}
	}

	/**
	 * Set Claude API key (called from preferences)
	 */
	public void setClaudeAPIKey(String apiKey) {
		if (apiKey != null && !apiKey.isEmpty()) {
			this.claudeAI = new ClaudeAIService(apiKey);
			logger.log(Level.INFO, "Claude AI service configured");
		} else {
			this.claudeAI = null;
		}
	}

	public JSONObject getLanguages() {
		JSONObject result = new JSONObject();
		try {
			List<Language> languages = LanguageUtils.getCommonLanguages();
			JSONArray array = new JSONArray();
			for (int i = 0; i < languages.size(); i++) {
				Language lang = languages.get(i);
				JSONObject json = new JSONObject();
				json.put("code", lang.getCode());
				json.put("description", lang.getDescription());
				array.put(json);
			}
			result.put("languages", array);
			result.put(Constants.STATUS, Constants.SUCCESS);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			logger.log(Level.ERROR, Messages.getString("AlignmentService.1"), e);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject getTypes() {
		JSONObject result = new JSONObject();
		JSONArray array = new JSONArray();
		String[] formats = FileFormats.getFormats();
		for (int i = 0; i < formats.length; i++) {
			if (!FileFormats.isBilingual(formats[i])) {
				JSONObject json = new JSONObject();
				json.put("code", FileFormats.getShortName(formats[i]));
				json.put("description", formats[i]);
				array.put(json);
			}
		}
		result.put("types", array);
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject getCharsets() {
		JSONObject result = new JSONObject();
		JSONArray array = new JSONArray();
		TreeMap<String, Charset> charsets = new TreeMap<>(Charset.availableCharsets());
		Set<String> keys = charsets.keySet();
		Iterator<String> i = keys.iterator();
		while (i.hasNext()) {
			Charset cset = charsets.get(i.next());
			JSONObject json = new JSONObject();
			json.put("code", cset.name());
			json.put("description", cset.displayName());
			array.put(json);
		}
		result.put("charsets", array);
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject getFileType(String file) {
		JSONObject result = new JSONObject();
		result.put("file", file);
		String detected = FileFormats.detectFormat(file);
		if (detected != null) {
			String type = FileFormats.getShortName(detected);
			if (type != null) {
				Charset charset = EncodingResolver.getEncoding(file, detected);
				if (charset != null) {
					result.put("charset", charset.name());
				}
			}
			result.put("type", type);
		}
		return result;
	}

	public JSONObject alignFiles(JSONObject json) {
		JSONObject result = new JSONObject();
		aligning = true;
		alignError = "";
		status = "";
		try {
			new Thread() {

				@Override
				public void run() {
					try {
						status = Messages.getString("AlignmentService.2");
						logger.log(Level.INFO, status);
						File srcXlf = File.createTempFile("file", ".xlf");
						srcXlf.deleteOnExit();
						File skl = File.createTempFile("file", ".skl");
						Map<String, String> params = new HashMap<>();
						params.put("source", json.getString("sourceFile"));
						params.put("srcLang", json.getString("srcLang"));
						params.put("xliff", srcXlf.getAbsolutePath());
						params.put("skeleton", skl.getAbsolutePath());
						params.put("format", FileFormats.getFullName(json.getString("srcType")));
						params.put("catalog", json.getString("catalog"));
						params.put("srcEncoding", json.getString("srcEnc"));
						params.put("paragraph", json.getBoolean("paragraph") ? "yes" : "no");
						params.put("srxFile", json.getString("srx"));
						params.put("xmlfilter", json.getString("xmlfilter"));
						List<String> res = Convert.run(params);
						if (!com.maxprograms.converters.Constants.SUCCESS.equals(res.get(0))) {
							alignError = res.get(1);
							status = "";
							aligning = false;
							return;
						}
						Files.delete(skl.toPath());

						status = Messages.getString("AlignmentService.3");
						logger.log(Level.INFO, status);
						File tgtXlf = File.createTempFile("file", ".xlf");
						tgtXlf.deleteOnExit();
						skl = File.createTempFile("file", ".skl");
						params = new HashMap<>();
						params.put("source", json.getString("targetFile"));
						params.put("srcLang", json.getString("tgtLang"));
						params.put("xliff", tgtXlf.getAbsolutePath());
						params.put("skeleton", skl.getAbsolutePath());
						params.put("format", FileFormats.getFullName(json.getString("tgtType")));
						params.put("catalog", json.getString("catalog"));
						params.put("srcEncoding", json.getString("tgtEnc"));
						params.put("paragraph", json.getBoolean("paragraph") ? "yes" : "no");
						params.put("srxFile", json.getString("srx"));
						params.put("xmlfilter", json.getString("xmlfilter"));
						res = Convert.run(params);
						if (!com.maxprograms.converters.Constants.SUCCESS.equals(res.get(0))) {
							alignError = res.get(1);
							status = "";
							aligning = false;
							return;
						}
						Files.delete(skl.toPath());

						status = Messages.getString("AlignmentService.4");
						logger.log(Level.INFO, status);

						Alignment algn = new Alignment(json.getString("srcLang"), json.getString("tgtLang"));
						algn.setFile(json.getString("alignmentFile"));

						SAXBuilder builder = new SAXBuilder();
						Document doc = builder.build(srcXlf);
						List<Element> list = new ArrayList<>();
						recurse(list, doc.getRootElement());
						algn.setSources(list);
						Files.delete(srcXlf.toPath());

						doc = builder.build(tgtXlf);
						list.clear();
						recurse(list, doc.getRootElement());
						algn.setTargets(list);
						Files.delete(tgtXlf.toPath());

						// NEW: Run Hunalign/Gale-Church alignment algorithm
						logger.log(Level.INFO, "Running alignment...");
						currentAlignmentResult = alignmentEngine.performAlignment(
							algn.getSources(),
							algn.getTargets()
						);

						// Store confidence scores and methods in alignment object
						for (AlignmentPair pair : currentAlignmentResult.getAllPairs()) {
							if (!pair.getSourceIndices().isEmpty()) {
								int segmentId = pair.getSourceIndices().get(0);
								algn.setConfidenceAndMethod(segmentId, pair.getConfidence(), pair.getNote());
							}
						}

						logger.log(Level.INFO, String.format(
							"Alignment complete: %d pairs, %.1f%% confidence, %d uncertain",
							currentAlignmentResult.getTotalPairs(),
							currentAlignmentResult.getOverallConfidence() * 100,
							currentAlignmentResult.getUncertainPairs().size()
						));

						algn.save();

						status = "";
						aligning = false;
						logger.log(Level.INFO, Messages.getString("AlignmentService.5"));
					} catch (IOException | SAXException | ParserConfigurationException e) {
						logger.log(Level.ERROR, e);
						alignError = e.getMessage();
						status = "";
						aligning = false;
					}
				}

				private void recurse(List<Element> list, Element e) {
					if (e.getName().equals("trans-unit")) {
						list.add(e.getChild("source"));
					} else {
						List<Element> children = e.getChildren();
						Iterator<Element> it = children.iterator();
						while (it.hasNext()) {
							recurse(list, it.next());
						}
					}
				}

			}.start();
			result.put(Constants.STATUS, Constants.SUCCESS);
			return result;
		} catch (IllegalThreadStateException e) {
			logger.log(Level.ERROR, e);
			alignError = e.getMessage();
			status = "";
			aligning = false;
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject alignmentStatus() {
		JSONObject result = new JSONObject();
		result.put("aligning", aligning);
		result.put("alignError", alignError);
		result.put("status", status);
		return result;
	}

	public JSONObject openFile(JSONObject json) {
		JSONObject result = new JSONObject();
		loading = true;
		loadError = "";
		status = Messages.getString("AlignmentService.6");
		try {
			new Thread() {

				@Override
				public void run() {
					try {
						alignment = new Alignment(json.getString("file"));

						// NEW: Run Hunalign/Gale-Church on existing alignment file
						logger.log(Level.INFO, "Analyzing existing alignment...");
						currentAlignmentResult = alignmentEngine.performAlignment(
							alignment.getSources(),
							alignment.getTargets()
						);

						// Store confidence scores in alignment object
						for (AlignmentPair pair : currentAlignmentResult.getAllPairs()) {
							if (!pair.getSourceIndices().isEmpty()) {
								int segmentId = pair.getSourceIndices().get(0);
								alignment.setConfidenceAndMethod(segmentId, pair.getConfidence(), pair.getNote());
							}
						}

						logger.log(Level.INFO, String.format(
							"Analysis complete: %d pairs, %.1f%% confidence, %d uncertain",
							currentAlignmentResult.getTotalPairs(),
							currentAlignmentResult.getOverallConfidence() * 100,
							currentAlignmentResult.getUncertainPairs().size()
						));

						status = "";
						loading = false;
					} catch (JSONException | SAXException | IOException | ParserConfigurationException e) {
						logger.log(Level.ERROR, e);
						loadError = e.getMessage();
						status = "";
						loading = false;
					}
				}
			}.start();
			result.put(Constants.STATUS, Constants.SUCCESS);
			return result;
		} catch (IllegalThreadStateException e) {
			logger.log(Level.ERROR, e);
			loadError = e.getMessage();
			status = "";
			loading = false;
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject loadingStatus() {
		JSONObject result = new JSONObject();
		result.put("loading", loading);
		result.put("loadError", loadError);
		result.put("status", status);
		return result;
	}

	public JSONObject getFileInfo() throws JSONException, SAXException, IOException, ParserConfigurationException {
		JSONObject result = alignment.getFileInfo();
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject getRows(JSONObject json) throws SAXException, IOException, ParserConfigurationException {
		JSONObject result = alignment.getRows(json);
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject exportTMX(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			alignment.exportTMX(json.getString("file"));
			result.put(Constants.STATUS, Constants.SUCCESS);
		} catch (JSONException | IOException | SAXException | ParserConfigurationException e) {
			logger.log(Level.ERROR, e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject saveFile() {
		JSONObject result = new JSONObject();
		saving = true;
		saveError = "";
		status = Messages.getString("AlignmentService.7");

		try {
			new Thread() {

				@Override
				public void run() {
					try {
						alignment.save();
						saving = false;
						status = "";
					} catch (JSONException | IOException e) {
						saving = false;
						logger.log(Level.ERROR, e);
						saveError = e.getMessage();
						status = "";
					}
				}
			}.start();
			result.put(Constants.STATUS, Constants.SUCCESS);
		} catch (IllegalThreadStateException e) {
			logger.log(Level.ERROR, e);
			saving = false;
			saveError = e.getMessage();
			status = "";
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject savingStatus() {
		JSONObject result = new JSONObject();
		result.put("saving", saving);
		result.put("saveError", saveError);
		result.put("status", status);
		return result;
	}

	public JSONObject removeTags() {
		JSONObject result = new JSONObject();
		alignment.removeTags();
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject exportCSV(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			alignment.exportCSV(json.getString("file"));
			result.put(Constants.STATUS, Constants.SUCCESS);
		} catch (IOException e) {
			logger.log(Level.ERROR, e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject exportExcel(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			alignment.exportExcel(json.getString("file"));
			result.put(Constants.STATUS, Constants.SUCCESS);
		} catch (IOException | JSONException | SAXException | ParserConfigurationException e) {
			logger.log(Level.ERROR, e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject setLanguages(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			alignment.setLanguages(json);
			result.put(Constants.STATUS, Constants.SUCCESS);
		} catch (JSONException | IOException | SAXException | ParserConfigurationException e) {
			logger.log(Level.ERROR, e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject closeFile() {
		JSONObject result = new JSONObject();
		alignment = null;
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject removeDuplicates() {
		JSONObject result = new JSONObject();
		alignment.removeDuplicates();
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject renameFile(JSONObject json) {
		JSONObject result = new JSONObject();
		alignment.setFile(json.getString("file"));
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject removeSegment(JSONObject json) {
		JSONObject result = new JSONObject();
		alignment.removeSegment(json);
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject segmentDown(JSONObject json) {
		JSONObject result = new JSONObject();
		alignment.segmentDown(json);
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject segmentUp(JSONObject json) {
		JSONObject result = new JSONObject();
		alignment.segmentUp(json);
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject mergeNext(JSONObject json) {
		JSONObject result = new JSONObject();
		alignment.mergeNext(json);
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject saveData(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			alignment.saveData(json);
			result.put(Constants.STATUS, Constants.SUCCESS);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			logger.log(Level.ERROR, e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	public JSONObject replaceText(JSONObject json) {
		JSONObject result = new JSONObject();
		alignment.replaceText(json);
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	public JSONObject splitSegment(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			alignment.splitSegment(json);
			result.put(Constants.STATUS, Constants.SUCCESS);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			logger.log(Level.ERROR, e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	// ==================== NEW: AI-ENHANCED ALIGNMENT METHODS ====================

	/**
	 * Get cost estimate for AI improvement
	 */
	public JSONObject estimateAICost() {
		JSONObject result = new JSONObject();
		try {
			if (alignment == null) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "No alignment file open");
				return result;
			}

			// Get uncertain segment IDs (both automatic and manual)
			List<Integer> uncertainIds = alignment.getUncertainSegmentIds();

			logger.log(Level.INFO, "Cost estimate: Found {0} uncertain segments", uncertainIds.size());

			if (uncertainIds.isEmpty()) {
				logger.log(Level.INFO, "No uncertain segments found for AI review");
				result.put(Constants.STATUS, Constants.SUCCESS);
				result.put("needsReview", false);
				result.put("cost", 0.0);
				result.put("pairsToReview", 0);
				result.put("inputTokens", 0);
				result.put("outputTokens", 0);
				result.put("totalTokens", 0);
				result.put("estimatedCost", 0.0);
				result.put("formattedCost", "$0.00");
				return result;
			}

			// Build AlignmentPairs for uncertain segments
			List<Element> sources = alignment.getSources();
			List<Element> targets = alignment.getTargets();
			List<AlignmentPair> uncertainPairs = new ArrayList<>();

			for (Integer segmentId : uncertainIds) {
				List<Integer> srcIndices = new ArrayList<>();
				List<Integer> tgtIndices = new ArrayList<>();

				if (segmentId < sources.size()) {
					srcIndices.add(segmentId);
				}
				if (segmentId < targets.size()) {
					tgtIndices.add(segmentId);
				}

				double confidence = alignment.getConfidence(segmentId);
				String note = alignment.getSegmentInfo(segmentId).manuallyMarked ?
					"Manually marked for review" : "Low confidence";

				uncertainPairs.add(new AlignmentPair(srcIndices, tgtIndices, confidence, note));
			}

			List<String> sourceStrings = alignmentEngine.getTextStrings(
				new ArrayList<>(sources)
			);
			List<String> targetStrings = alignmentEngine.getTextStrings(
				new ArrayList<>(targets)
			);

			CostEstimate estimate = CostEstimator.estimateCost(
				sourceStrings,
				targetStrings,
				uncertainPairs
			);

			result.put(Constants.STATUS, Constants.SUCCESS);
			result.put("needsReview", true);
			result.put("pairsToReview", estimate.getPairsToReview());
			result.put("inputTokens", estimate.getInputTokens());
			result.put("outputTokens", estimate.getOutputTokens());
			result.put("totalTokens", estimate.getTotalTokens());
			result.put("estimatedCost", estimate.getTotalCost());
			result.put("formattedCost", estimate.getFormattedCost());
			result.put("breakdown", estimate.getDetailedBreakdown());

		} catch (Exception e) {
			logger.log(Level.ERROR, "Error estimating AI cost", e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	/**
	 * Improve alignment using Claude AI (only uncertain pairs)
	 */
	public JSONObject improveAlignmentWithAI() {
		JSONObject result = new JSONObject();
		try {
			if (claudeAI == null) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "Claude AI not configured. Please set API key in preferences.");
				return result;
			}

			if (alignment == null) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "No alignment file open");
				return result;
			}

			// Get uncertain segment IDs (both automatic and manual)
			List<Integer> uncertainIds = alignment.getUncertainSegmentIds();

			if (uncertainIds.isEmpty()) {
				result.put(Constants.STATUS, Constants.SUCCESS);
				result.put("improved", 0);
				result.put("message", "No uncertain alignments to improve");
				return result;
			}

			// Build AlignmentPairs for uncertain segments
			List<Element> sources = alignment.getSources();
			List<Element> targets = alignment.getTargets();
			List<AlignmentPair> uncertainPairs = new ArrayList<>();

			for (Integer segmentId : uncertainIds) {
				List<Integer> srcIndices = new ArrayList<>();
				List<Integer> tgtIndices = new ArrayList<>();

				if (segmentId < sources.size()) {
					srcIndices.add(segmentId);
				}
				if (segmentId < targets.size()) {
					tgtIndices.add(segmentId);
				}

				double confidence = alignment.getConfidence(segmentId);
				String note = alignment.getSegmentInfo(segmentId).manuallyMarked ?
					"Manually marked for review" : "Low confidence";

				uncertainPairs.add(new AlignmentPair(srcIndices, tgtIndices, confidence, note));
			}

			List<String> sourceStrings = alignmentEngine.getTextStrings(
				new ArrayList<>(sources)
			);
			List<String> targetStrings = alignmentEngine.getTextStrings(
				new ArrayList<>(targets)
			);

			// Call Claude AI to improve uncertain pairs
			List<AlignmentPair> improvedPairs = claudeAI.improveAlignment(
				sourceStrings,
				targetStrings,
				uncertainPairs
			);

			// Apply AI improvements: reorder targets if needed
			logger.log(Level.INFO, "Applying AI improvements...");
			int reorderedCount = 0;

			for (AlignmentPair pair : improvedPairs) {
				if (pair.getSourceIndices().isEmpty() || pair.getTargetIndices().isEmpty()) {
					continue;
				}

				int sourceId = pair.getSourceIndices().get(0);
				int suggestedTargetId = pair.getTargetIndices().get(0);

				// Check if AI suggests a different target index
				if (sourceId != suggestedTargetId) {
					// AI wants to align sourceId with a different target
					logger.log(Level.INFO, "AI suggests: S{0} -> T{1} (was T{2})",
						new Object[]{sourceId, suggestedTargetId, sourceId});

					// Swap target segments to align correctly
					if (sourceId < targets.size() && suggestedTargetId < targets.size()) {
						Element temp = targets.get(sourceId);
						targets.set(sourceId, targets.get(suggestedTargetId));
						targets.set(suggestedTargetId, temp);
						reorderedCount++;
						logger.log(Level.INFO, "Swapped T{0} <-> T{1}",
							new Object[]{sourceId, suggestedTargetId});
					}
				}

				// Update confidence and flags
				alignment.setConfidence(sourceId, pair.getConfidence());
				alignment.setAIReviewed(sourceId, true);
				alignment.setManualMark(sourceId, false);
			}

			// Save the reordered targets back to alignment
			if (reorderedCount > 0) {
				alignment.setTargets(targets);
				logger.log(Level.INFO, "Reordered {0} target segments", reorderedCount);
			}

			// Calculate statistics
			int remainingUncertain = alignment.getUncertainSegmentIds().size();
			int totalSegments = Math.max(sources.size(), targets.size());
			double overallConfidence = (totalSegments - remainingUncertain) / (double) totalSegments;

			result.put(Constants.STATUS, Constants.SUCCESS);
			result.put("improved", improvedPairs.size());
			result.put("remainingUncertain", remainingUncertain);
			result.put("overallConfidence", overallConfidence);

		} catch (IOException e) {
			logger.log(Level.ERROR, "Error improving alignment with AI", e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, "AI service error: " + e.getMessage());
		} catch (Exception e) {
			logger.log(Level.ERROR, "Unexpected error during AI improvement", e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	/**
	 * Get current alignment statistics
	 */
	public JSONObject getAlignmentStats() {
		JSONObject result = new JSONObject();
		if (currentAlignmentResult != null) {
			result = currentAlignmentResult.toJSON();
		}
		result.put(Constants.STATUS, Constants.SUCCESS);
		return result;
	}

	/**
	 * Apply improved alignment result to actual alignment object
	 */
	private void applyImprovedAlignment(AlignmentResult alignResult) throws Exception {
		// This would reconstruct the alignment from pairs
		// For now, we just log - full implementation would update
		// the actual sources/targets lists in the Alignment object
		logger.log(Level.INFO, "Applied improved alignment: {0}", alignResult.toString());

		// TODO: Implement full reconstruction if needed
		// For MVP, the UI can trigger a refresh after AI improvement
	}

	/**
	 * Toggle manual uncertainty marking for a segment
	 */
	public JSONObject toggleManualMark(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			if (alignment == null) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "No alignment file open");
				return result;
			}

			int segmentId = json.getInt("segmentId");
			alignment.toggleManualMark(segmentId);

			result.put(Constants.STATUS, Constants.SUCCESS);
			result.put("segmentId", segmentId);
			result.put("manuallyMarked", alignment.getSegmentInfo(segmentId).manuallyMarked);

		} catch (Exception e) {
			logger.log(Level.ERROR, "Error toggling manual mark", e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	/**
	 * Move target segment up (swap with previous)
	 */
	public JSONObject moveTargetUp(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			if (alignment == null) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "No alignment file open");
				return result;
			}

			int segmentId = json.getInt("segmentId");
			if (segmentId <= 0) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "Cannot move first segment up");
				return result;
			}

			List<Element> targets = alignment.getTargets();
			if (segmentId >= targets.size()) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "Segment index out of range");
				return result;
			}

			// Swap with previous
			Element temp = targets.get(segmentId);
			targets.set(segmentId, targets.get(segmentId - 1));
			targets.set(segmentId - 1, temp);
			alignment.setTargets(targets);

			result.put(Constants.STATUS, Constants.SUCCESS);
			result.put("segmentId", segmentId);

		} catch (Exception e) {
			logger.log(Level.ERROR, "Error moving target up", e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	/**
	 * Move target segment down (swap with next)
	 */
	public JSONObject moveTargetDown(JSONObject json) {
		JSONObject result = new JSONObject();
		try {
			if (alignment == null) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "No alignment file open");
				return result;
			}

			int segmentId = json.getInt("segmentId");
			List<Element> targets = alignment.getTargets();

			if (segmentId >= targets.size() - 1) {
				result.put(Constants.STATUS, Constants.ERROR);
				result.put(Constants.REASON, "Cannot move last segment down");
				return result;
			}

			// Swap with next
			Element temp = targets.get(segmentId);
			targets.set(segmentId, targets.get(segmentId + 1));
			targets.set(segmentId + 1, temp);
			alignment.setTargets(targets);

			result.put(Constants.STATUS, Constants.SUCCESS);
			result.put("segmentId", segmentId);

		} catch (Exception e) {
			logger.log(Level.ERROR, "Error moving target down", e);
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}

	/**
	 * Test Claude AI connection
	 */
	public JSONObject testClaudeConnection() {
		JSONObject result = new JSONObject();
		if (claudeAI == null) {
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, "Claude AI not configured");
			return result;
		}

		try {
			boolean connected = claudeAI.testConnection();
			result.put(Constants.STATUS, connected ? Constants.SUCCESS : Constants.ERROR);
			result.put("connected", connected);
		} catch (Exception e) {
			result.put(Constants.STATUS, Constants.ERROR);
			result.put(Constants.REASON, e.getMessage());
		}
		return result;
	}
}
