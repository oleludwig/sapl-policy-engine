package io.sapl.pdp.interceptors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.sapl.interpreter.CombinedDecision;
import io.sapl.pdp.PDPDecision;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ReportTextRenderUtil {

	public static String textReport(JsonNode jsonReport, boolean prettyPrint, ObjectMapper mapper) {
		StringBuilder report = new StringBuilder("--- The PDP made a decision ---\n");
		report.append("Subscription: ").append(prettyPrint ? '\n' : "").append(prettyPrintJson(jsonReport.get(PDPDecision.AUTHORIZATION_SUBSCRIPTION), prettyPrint, mapper)).append('\n');
		report.append("Decision    : ").append(prettyPrint ? '\n' : "").append(prettyPrintJson(jsonReport.get(PDPDecision.AUTHORIZATION_DECISION), prettyPrint, mapper)).append('\n');
		report.append("Timestamp   : ").append(jsonReport.get(PDPDecision.TIMESTAMP_STRING).textValue()).append('\n');
		report.append("Algorithm   : ").append(jsonReport.get(ReportBuilderUtil.PDP_COMBINING_ALGORITHM)).append('\n');
		var topLevelError = jsonReport.get(CombinedDecision.ERROR);
		if (topLevelError != null) {
			report.append("PDP Error   : ").append(topLevelError).append('\n');
		}
		var matchingDocuments = jsonReport.get(PDPDecision.MATCHING_DOCUMENTS);
		if (matchingDocuments == null || !matchingDocuments.isArray() || matchingDocuments.size() == 0) {
			report.append("Matches     : NONE (i.e.,no policies/policy sets were set, or all target expressions evaluated to false or error.)\n");
		} else {
			report.append("Matches     : ").append(matchingDocuments).append('\n');
		}
		var modifications = jsonReport.get(PDPDecision.MODIFICATIONS_STRING);
		if (modifications != null && modifications.isArray() && modifications.size() != 0) {
			report.append("There were interceptors invoked after the PDP which changed the decision:\n");
			for (var mod : modifications) {
				report.append(" - ").append(mod).append('\n');
			}
		}
		var documentReports = jsonReport.get("documentReports");
		if (documentReports == null || !documentReports.isArray() || documentReports.size() == 0) {
			report.append("No policy or policy sets have been evaluated\n");
			return report.toString();
		}

		for (var documentReport : documentReports) {
			report.append(documentReport(documentReport));
		}
		return report.toString();
	}

	private static String documentReport(JsonNode documentReport) {
		var documentType = documentReport.get("documentType");
		if (documentType == null)
			return "Reporting error. Unknown documentType: " + documentReport + '\n';
		var type = documentType.asText();
		if ("policy set".equals(type))
			return policySetReport(documentReport);
		if ("policy".equals(type))
			return policyReport(documentReport);

		return "Reporting error. Unknown documentType: " + type + '\n';
	}

	private static String policyReport(JsonNode policy) {
		var report = "Policy Evaluation Result ===================\n";
		report += "Name        : " + policy.get("documentName") + '\n';
		report += "Entitlement : " + policy.get("entitlement") + '\n';
		report += "Decision    : " + policy.get(PDPDecision.AUTHORIZATION_DECISION) + '\n';
		if (policy.has("target"))
			report += "Target      : " + policy.get("target") + '\n';
		if (policy.has("where"))
			report += "Where       : " + policy.get("where") + '\n';
		report += errorReport(policy.get("errors"));
		report += attributeReport(policy.get("attributes"));
		return report;
	}

	private static String errorReport(JsonNode errors) {
		var report = new StringBuilder();
		if (errors == null || !errors.isArray() || errors.size() == 0) {
			return report.toString();
		}
		report.append("Errors during evaluation:\n");
		for (var error : errors) {
			report.append(" - ");
			report.append(error);
			report.append('\n');
		}
		return report.toString();
	}

	private static String attributeReport(JsonNode attributes) {
		var report = new StringBuilder();
		if (attributes == null || !attributes.isArray() || attributes.size() == 0) {
			return report.toString();
		}
		report.append("Policy Information Point Data:\n");
		for (var attribute : attributes) {
			report.append(" - ");
			report.append(attribute);
			report.append('\n');
		}
		return report.toString();
	}

	private static String policySetReport(JsonNode policySet) {
		StringBuilder report = new StringBuilder("Policy Set Evaluation Result ===============\n");
		report.append("Name        : ").append(policySet.get("documentName")).append('\n');
		report.append("Algorithm   : ").append(policySet.get(CombinedDecision.COMBINING_ALGORITHM)).append('\n');
		report.append("Decision    : ").append(policySet.get(PDPDecision.AUTHORIZATION_DECISION)).append('\n');
		if (policySet.has("target"))
			report.append("Target      : ").append(policySet.get("target")).append('\n');
		if (policySet.has("errorMessage"))
			report.append("Error       : ").append(policySet.get("errorMessage")).append('\n');
		var evaluatedPolicies = policySet.get("evaluatedPolicies");
		if (evaluatedPolicies != null && evaluatedPolicies.isArray()) {
			for (var policyReport : evaluatedPolicies) {
				report.append(indentText("   |", policyReport(policyReport)));
			}
		}
		return report.toString();
	}

	private String indentText(String indentation, String text) {
		StringBuilder indented = new StringBuilder();
		for (var line : text.split("\n")) {
			indented.append(indentation).append(line.replace("\r", "")).append('\n');
		}
		return indented.toString();
	}

	public static String prettyPrintJson(JsonNode json, boolean prettyPrint, ObjectMapper mapper) {
		if (!prettyPrint)
			return json.toString();
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
		} catch (JsonProcessingException e) {
			log.error("Pretty print JSON failed: ", e);
			return json.toString();
		}
	}

}