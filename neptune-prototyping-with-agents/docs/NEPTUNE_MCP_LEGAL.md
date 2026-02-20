# Neptune MCP Server - Legal and Compliance Documentation

This document provides legal approval and compliance information for the use of the Neptune MCP Server in this sample application.

## Right to Use Verification

### License

The Neptune MCP Server is part of the AWS MCP Servers project, which is licensed under the **Apache License 2.0**.

- **Project**: AWS MCP Servers (awslabs/mcp)
- **License**: Apache-2.0
- **Source**: https://github.com/awslabs/mcp
- **License File**: https://github.com/awslabs/mcp/blob/main/LICENSE

### Apache 2.0 License Summary

The Apache 2.0 license grants users the right to:
- Use the software for any purpose
- Distribute the software
- Modify the software
- Distribute modified versions of the software

**Key Requirements:**
- Include a copy of the Apache 2.0 license
- State significant changes made to the software
- Include a NOTICE file if one is provided by the original project
- Preserve copyright, patent, trademark, and attribution notices

### Verification

This sample application complies with the Apache 2.0 license by:
1. Using the Neptune MCP Server as provided by AWS Labs
2. Not modifying the Neptune MCP Server source code
3. Running the server via `uvx` which automatically fetches the latest published version
4. Including this documentation to acknowledge the license and source

## Distribution Rights for Sample Code

### This Sample Application

This sample application (Neptune AI Agents) is separate from the Neptune MCP Server and has its own licensing terms as specified in the [LICENSE](../LICENSE) file in the root of this repository.

### Neptune MCP Server Distribution

The Neptune MCP Server is distributed through:
- **PyPI**: `awslabs.amazon-neptune-mcp-server`
- **Installation Method**: `uvx awslabs.amazon-neptune-mcp-server@latest`

This sample does not redistribute the Neptune MCP Server binaries or source code. Instead, it:
- Documents how to use the server via `uvx`
- Provides configuration examples
- Integrates with the server through the Model Context Protocol (MCP) standard interface

### Compliance with Distribution Terms

This sample complies with Apache 2.0 distribution requirements by:
- Not bundling or redistributing Neptune MCP Server code
- Directing users to install from official sources (PyPI via `uvx`)
- Providing proper attribution to AWS Labs
- Including this legal documentation

## Security Assessment of the MCP Server

### Official AWS Project

The Neptune MCP Server is an **official AWS Labs project**, providing a level of trust and security assurance:

- **Maintainer**: AWS Labs (Amazon Web Services)
- **Repository**: https://github.com/awslabs/mcp
- **Security Policy**: https://github.com/awslabs/mcp/security/policy
- **Code of Conduct**: https://github.com/awslabs/mcp/blob/main/CODE_OF_CONDUCT.md

### Security Considerations

**Strengths:**
1. **Official AWS Project**: Developed and maintained by AWS
2. **Open Source**: Code is publicly auditable
3. **Active Maintenance**: Regular updates and security patches
4. **Community Support**: Large contributor base (246+ contributors)
5. **Security Reporting**: Established process for reporting security issues

**Risks and Mitigations:**

| Risk | Mitigation |
|------|------------|
| Dependency vulnerabilities | Use `uvx` which automatically uses latest versions; monitor security advisories |
| Local process compromise | Run with minimal required permissions; use isolated environments |
| Credential exposure | Use IAM roles when possible; never hardcode credentials |
| Network interception | MCP server uses local stdio transport; Neptune connections use TLS |

### Security Best Practices for This Sample

1. **Keep Updated**: `uvx` automatically uses the latest version of the Neptune MCP Server
2. **Least Privilege**: Configure IAM policies with minimum required permissions
3. **Network Security**: Use VPC-only Neptune access or restrict public endpoints to specific IPs
4. **Audit Logging**: Enable CloudTrail and Neptune audit logs
5. **Environment Isolation**: Test in development environments before production use

### Vulnerability Reporting

Security issues with the Neptune MCP Server should be reported according to AWS's security policy:
- See: https://github.com/awslabs/mcp/security/policy
- For this sample application, see: [CONTRIBUTING.md](../CONTRIBUTING.md#security-issue-notifications)

## Data Handling and Privacy Compliance

### Data Flow

The Neptune MCP Server facilitates the following data flows:

```
User Input → AI Agent → Neptune MCP Server → Amazon Neptune
                ↓
         Amazon Bedrock
```

**Data Types:**
- Natural language prompts and queries
- Graph database queries (openCypher)
- Graph data (nodes, edges, properties)
- Query results and statistics

### Data Storage

**Neptune MCP Server:**
- **Does NOT store** user data persistently
- Operates as a stateless proxy between agents and Neptune
- Runs locally on the user's machine
- Uses stdio transport (no network exposure)

**This Sample Application:**
- Stores output files locally in `output/` directory
- Files may contain sensitive information (use cases, models, queries, data patterns)
- No data is sent to external services except:
  - Amazon Bedrock (for AI model invocations)
  - Amazon Neptune (for graph database operations)

### AWS Service Data Handling

**Amazon Bedrock:**
- Does not store or log prompts or model outputs for base models
- Data is not used to train or improve base models
- All data in transit is encrypted using TLS 1.2+
- See: [Amazon Bedrock Data Privacy](https://docs.aws.amazon.com/bedrock/latest/userguide/data-protection.html)

**Amazon Neptune:**
- Data is stored in your AWS account
- You control data retention and deletion
- Supports encryption at rest with AWS KMS
- Supports encryption in transit with TLS
- See: [Amazon Neptune Security](https://docs.aws.amazon.com/neptune/latest/userguide/security.html)

### Privacy Compliance Considerations

**GDPR Compliance:**
- Data is processed in AWS regions you specify
- You control data retention and deletion
- Neptune supports data encryption and access controls
- Bedrock does not retain customer data for base models

**HIPAA Compliance:**
- Amazon Neptune is HIPAA eligible
- Amazon Bedrock is HIPAA eligible
- Ensure proper Business Associate Agreements (BAA) are in place
- Follow HIPAA security and privacy requirements

**Data Residency:**
- All data processing occurs in the AWS region you configure
- Neptune MCP Server runs locally (no data leaves your environment except to AWS services)
- Configure `AWS_REGION` environment variable to control data location

### Recommendations for Sensitive Data

1. **Classify Data**: Identify what data is sensitive before using this sample
2. **Encrypt at Rest**: Enable Neptune encryption and use encrypted file systems for local files
3. **Encrypt in Transit**: Verify TLS is enabled for all connections
4. **Access Controls**: Use IAM policies to restrict access to Neptune and Bedrock
5. **Audit Logging**: Enable CloudTrail and Neptune audit logs
6. **Data Minimization**: Only generate and store data necessary for your use case
7. **Secure Deletion**: Use secure deletion methods for local output files
8. **Regular Reviews**: Periodically review data handling practices and compliance

## Compliance Checklist

- [x] **License Compliance**: Apache 2.0 license terms followed
- [x] **Attribution**: AWS Labs credited as Neptune MCP Server maintainer
- [x] **Distribution**: No redistribution of MCP server binaries; users install from official sources
- [x] **Security Assessment**: Documented security considerations and best practices
- [x] **Data Privacy**: Documented data flows and privacy considerations
- [x] **Vulnerability Reporting**: Established process for security issues
- [x] **Documentation**: Comprehensive legal and compliance documentation provided

## Additional Resources

- [AWS MCP Servers GitHub Repository](https://github.com/awslabs/mcp)
- [Apache License 2.0 Full Text](https://www.apache.org/licenses/LICENSE-2.0)
- [Amazon Neptune Security Documentation](https://docs.aws.amazon.com/neptune/latest/userguide/security.html)
- [Amazon Bedrock Data Privacy](https://docs.aws.amazon.com/bedrock/latest/userguide/data-protection.html)
- [AWS Compliance Programs](https://aws.amazon.com/compliance/programs/)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)

## Document Version

- **Version**: 1.0
- **Last Updated**: 2026-01-26
- **Next Review**: 2026-07-26 (6 months)

## Contact

For questions about this documentation or compliance concerns:
- See [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines
- See [CONTRIBUTING.md#security-issue-notifications](../CONTRIBUTING.md#security-issue-notifications) for security reporting

---

## Legal

**Copyright © 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.**

**License:** MIT-0 (MIT No Attribution)

This sample code is made available under the MIT-0 license. See the [LICENSE](../LICENSE) file for details.

**Terms of Use:**
- This is sample code for demonstration and educational purposes
- Not intended for production use without proper review and testing
- No warranties or guarantees provided
- Users are responsible for testing, validating, and adapting the code for their use cases
- AWS service usage will incur costs according to AWS pricing
