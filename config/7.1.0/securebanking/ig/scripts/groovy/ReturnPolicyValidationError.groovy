SCRIPT_NAME = "[ReturnPolicyValidationError] - "
logger.debug(SCRIPT_NAME + "Running...")

def response = new Response(Status.UNAUTHORIZED);
message = "policy_validation_failed"
logger.error(SCRIPT_NAME + message)
response.setEntity("{ \"error\":\"" + message + "\"}")

return response