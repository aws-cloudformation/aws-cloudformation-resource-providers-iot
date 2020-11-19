# AWS::IoT::Dimension

## Running Contract Tests

You can execute the following commands to run the tests.
You will need to have docker installed and running.

```bash
# Package the code with Maven
mvn package
# Start SAM which will execute lambdas in Docker
sam local start-lambda

# In a separate terminal, run the contract tests
cfn test --enforce-timeout 240

# Execute a single test
cfn test --enforce-timeout 240 -- -k <testname>
```
