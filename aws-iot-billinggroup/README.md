# AWS::IoT::BillingGroup

### Building Package

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

```
# Package the code with Maven
mvn clean && mvn package
```

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.

### Running Contract Tests

You can execute the following commands to run the tests. You will need to have docker installed and running.
```
# Package the code with Maven
mvn package

# Start SAM which will execute lambdas in Docker
sam local start-lambda

# In a separate terminal, run the contract tests
cfn test --enforce-timeout 240

# Execute a single test
cfn test --enforce-timeout 240 -- -k <testname>
```
List of CloudFormation contract tests can be found here : https://github.com/aws-cloudformation/cloudformation-cli/tree/master/src/rpdk/core/contract/suite
