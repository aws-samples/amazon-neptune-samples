from vss_integration import VSSIntegration

class KGQueryExecutor:

    def __init__(self) -> None:
        self.edgar_graph = VSSIntegration()

    def load_edgar_data(self):
        edgar_data = self.edgar_graph.load_data()
        print(edgar_data)
    
    def query_top_companies_with_largest_holdings(self):
        top_companies_query = self.edgar_graph.fetch_top_ten_companies_with_largest_holding()
        print(top_companies_query)
    
    def query_number_of_holdings_by_top_investor(self):
        investor_holdings_query = self.edgar_graph.fetch_investor_holdings()
        print(investor_holdings_query)
    
    def query_top_investments_of_top_investor(self):
        top_investments_query = self.edgar_graph.fetch_top_ten_investments()
        print(top_investments_query)
    
    def query_investments_greater_than_billion_by_top_investor(self):
        high_value_investments_query = self.edgar_graph.fetch_investments_greater_than_billion()
        print(high_value_investments_query)
    
    def query_shared_investments_exceeding_billion(self):
        shared_investments_query = self.edgar_graph.fetch_shared_investments_greater_than_billion()
        print(shared_investments_query)
    
    def query_jaccard_similarity_top_ten_investments(self):
        jaccard_similarity_query = self.edgar_graph.js_compare_stocks_and_investments_for_top_ten()
        print(jaccard_similarity_query)
    
    def query_jaccard_similarity_entire_portfolio(self):
        jaccard_similarity_portfolio_query = self.edgar_graph.js_compare_stocks_and_investments_for_entire_portfolio()
        print(jaccard_similarity_portfolio_query)
    
    def query_top_similar_holders(self):
        top_similar_holders_query = self.edgar_graph.fetch_top_ten_similar_holders()
        print(top_similar_holders_query)
    
    def query_shared_holders_greater_than_billion(self):
        shared_holders_query = self.edgar_graph.fetch_shared_holders_greater_than_billion()
        print(shared_holders_query)

    def query_jaccard_similarity_competitor_portfolios(self):
        jaccard_similarity_competitors_query = self.edgar_graph.js_on_identified_competitors()
        print(jaccard_similarity_competitors_query)

def main():
    print("hello World")
    query_executor = KGQueryExecutor()
    # query_executor.query_top_companies_with_largest_holdings()
    # query_executor.query_number_of_holdings_by_top_investor()
    # query_executor.query_top_investments_of_top_investor()
    # query_executor.query_investments_greater_than_billion_by_top_investor()
    # query_executor.query_shared_investments_exceeding_billion()
    # query_executor.query_jaccard_similarity_top_ten_investments()
    # query_executor.query_jaccard_similarity_entire_portfolio()
    # query_executor.query_top_similar_holders()
    # query_executor.query_shared_holders_greater_than_billion()
    # query_executor.query_jaccard_similarity_competitor_portfolios()

if __name__ == "__main__":
    main()
