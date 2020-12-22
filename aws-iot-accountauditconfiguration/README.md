# AWS::IoT::AccountAuditConfiguration

## Running Contract Tests

You can execute the following commands to run the tests.
You will need to have docker installed and running.

```bash
# Create a CloudFormation stack with contract test dependencies (an IAM Role)
aws cloudformation deploy  \
--stack-name cfn-contract-test-dependencies-account-audit-configuration \
--template-file packaging_additional_published_artifacts/contract_test_dependencies.yml \
--capabilities CAPABILITY_IAM \
--region us-east-1

# Package the code with Maven
mvn package
# Start SAM which will execute lambdas in Docker
sam local start-lambda

# In a separate terminal, run the contract tests
cfn test --enforce-timeout 240

# Execute a single test
cfn test --enforce-timeout 240 -- -k <testname>
```
