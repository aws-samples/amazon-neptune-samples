# Amazon Neptune Graphs and Jupyter Notebooks

__[August 2021]__ The Neptune-SageMaker examples in this repository have been deprecated in favour of the [Amazon Neptune Workbench](https://docs.aws.amazon.com/neptune/latest/userguide/graph-notebooks.html). We recommend that you use the Workbench for all new Neptune notebook development. Alternatively, you can create your own notebooks using the [_neptune-python-utils_ library](https://github.com/awslabs/amazon-neptune-tools/tree/master/neptune-python-utils). Note that _neptune-python-utils_ supports Gremlin Python 3.5.x. As such, it is not compatible with the Neptune Workbench, which currently supports 3.4.x.

<del>Whether you’re creating a new graph data model and queries, or exploring an existing graph dataset, it can be useful to have an interactive query environment that allows you to visualize the results. This [directory](neptune-sagemaker/README.md) has samples from two blog posts to show you how to achieve this by connecting an Amazon SageMaker notebook to an Amazon Neptune database. Using the notebook, you load data into the database, query it and visualize the results.</del>

- <del>[Blog Post: Analyze Amazon Neptune Graphs Using Amazon Sagemaker Jupyter Notebooks](https://aws.amazon.com/blogs/database/analyze-amazon-neptune-graphs-using-amazon-sagemaker-jupyter-notebooks/)</del>
- <del>[Blog Post: Let Me Graph that for You - Part 1 - Air Routes](https://aws.amazon.com/blogs/database/let-me-graph-that-for-you-part-1-air-routes/)</del>

## Contents
1. [Jupyter Notebook Examples](notebooks)
1. [CloudFormation Templates](cloudformation-templates)
1. [Lambda Scripts](lambda)
1. [Utility Scripts](scripts)

## License
These samples are available under the [MIT License](LICENSE.txt).

