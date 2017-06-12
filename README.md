# EC2VolumeSnapshotter

Third major iteration of this script. Just a rewrite into Kotlin.

## Usage

### Set the environment
The environment needs to be set properly to run:
http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html

_~/.aws/credentials_

```
[default]
aws_access_key_id = your_access_key_id
aws_secret_access_key = your_secret_access_key
```

_~/.aws/config_

```
[default]
region = your_aws_region
```

### Create the config
Create a new YAML config file. The `name`, `volId` and `minSnapshots` are all required. Currently only 1 region is supported, so make sure all volume IDs are present in the AWS account and region configured in the AWS files.

_config.yml_

```yaml
- name: "Website Server Root"
  volId: "vol-00000000000000000"
  minSnapshots: 5
- name: "Website Server Data"
  volId: "vol-00000000000000000"
  minSnapshots: 5
- name: "Workhorse Server Root"
  volId: "vol-00000000000000000"
  minSnapshots: 5
```

### Build:

```
gradle clean build
```

### Run the JAR

```
java -jar build/libs/ec2volumesnapshotter-1.0-SNAPSHOT.jar config.yml
```
