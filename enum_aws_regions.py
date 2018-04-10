from enum import Enum


# Country Code is based on "country code ISO 3166-1 alpha-2"
# name = "Region Name" in table at https://docs.aws.amazon.com/general/latest/gr/rande.html#ec2_region
# descriptive_name = Region Name in https://aws.amazon.com/about-aws/global-infrastructure/regional-product-services/
# https://pypi.python.org/pypi/incf.countryutils
class AwsRegions(Enum):
    US_EAST_1 = {"code": "us-east-1", "name": "N. Virginia", "descriptive_name": "Northern Virginia",
                 "country_code": "US"}
    US_EAST_2 = {"code": "us-east-2", "name": "Ohio", "descriptive_name": "Ohio", "country_code": "US"}
    US_WEST_1 = {"code": "us-west-1", "name": "N. California", "descriptive_name": "Northern California",
                 "country_code": "US"}
    US_WEST_2 = {"code": "us-west-2", "name": "Oregon", "descriptive_name": "Oregon", "country_code": "US"}
    CA_CENTRAL_1 = {"code": "ca-central-1", "name": "Canada Central", "descriptive_name": "Montreal",
                    "country_code": "CA"}
    SA_EAST_1 = {"code": "sa-east-1", "name": "Sao Paulo", "descriptive_name": "São Paulo", "country_code": "BR"}
    EU_WEST_1 = {"code": "eu-west-1", "name": "Ireland", "descriptive_name": "Ireland", "country_code": "IE"}
    EU_WEST_2 = {"code": "eu-west-2", "name": "London", "descriptive_name": "London", "country_code": "GB"}
    EU_WEST_3 = {"code": "eu-west-3", "name": "Paris", "descriptive_name": "Paris", "country_code": "FR"}
    EU_CENTRAL_1 = {"code": "eu-central-1", "name": "Frankfurt", "descriptive_name": "Frankfurt", "country_code": "DE"}
    AP_SOUTHEAST_1 = {"code": "ap-southeast-1", "name": "Singapore", "descriptive_name": "Singapore",
                      "country_code": "SG"}
    AP_SOUTHEAST_2 = {"code": "ap-southeast-2", "name": "Sydney", "descriptive_name": "Sydney", "country_code": "AU"}
    AP_NORTHEAST_1 = {"code": "ap-northeast-1", "name": "Tokyo", "descriptive_name": "Tokyo", "country_code": "JP"}
    AP_NORTHEAST_2 = {"code": "ap-northeast-2", "name": "Seoul", "descriptive_name": "Seoul", "country_code": "KR"}
    AP_NORTHEAST_3 = {"code": "ap-northeast-3", "name": "Osaka-Local", "descriptive_name": "Osaka",
                      "country_code": "JP"}
    AP_SOUTH_1 = {"code": "ap-south-1", "name": "Mumbai", "descriptive_name": "Mumbai", "country_code": "IN"}
