from enum import Enum


class VertexEdgeLabels(Enum):
    vertex_label_continent = 'continent'
    vertex_label_country = 'country'
    vertex_label_aws_region = 'aws_region'
    vertex_label_aws_service = 'aws_service'
    vertex_label_aws_product = 'product'
    vertex_label_offer_term = 'offer_term'
    
    edge_label_country_to_continent = 'is_located_in_continent'
    edge_label_awsRegion_to_country = 'is_located_in_country'
    edge_label_awsRegion_to_awsService = 'offers_service'
    edge_label_awsRegion_to_offerTerm = 'for_term'
    edge_label_offerTerm_to_product = 'charges'
    edge_label_product_to_awsService = 'is_product_of'
