from socket import gaierror
import gremlin_interface
import sys
import os
import csv


def write_dict_csv(csv_file, csv_columns, dict_data_list):
    try:
        with open(csv_file, 'w') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=csv_columns)
            writer.writeheader()
            for data in dict_data_list:
                writer.writerow(data)
    except IOError as err:
        print("I/O error: {0}".format(err))    
    return 


def remove_non_exsistent_labels(labels_filter_list, labels_in_database):
    for label in labels_filter_list:
        if label not in labels_in_database:
            labels_filter_list.remove(label)
            print (label, ':label does not exist and will not be exported')
    return labels_filter_list


def export_edges(graph_traversal, export_folder_path, labels_filter_list=[]):
    print('Exporting Edges')
    all_edge_labels_in_database = graph_traversal.E().label().dedup().toList()
    if len(labels_filter_list) == 0:
        edge_label_list = all_edge_labels_in_database
    else:
        edge_label_list = remove_non_exsistent_labels(labels_filter_list, all_edge_labels_in_database)
    edge_label_list.sort(key=str.lower)
    for edge_label in edge_label_list:
        print('Exporting Edges with Label:', edge_label)
        edges_id_list = gremlin_traversal.E().hasLabel(edge_label).id().toList()
        edge_attribute_names_set = set()
        edge_export_list = []
        for edge_id in edges_id_list:
            edge_attr_dict = graph_traversal.E(edge_id).valueMap().next()
            for attr_key in edge_attr_dict.keys():
                edge_attr_dict[attr_key] = ','.join(map(str, edge_attr_dict[attr_key]))
            edge_attr_dict['~id'] = edge_id
            edge_attr_dict['~label'] = edge_label
            edge_attr_dict['~from'] = graph_traversal.E(edge_id).outV().id().next()
            edge_attr_dict['~to'] = graph_traversal.E(edge_id).inV().id().next()
            edge_attribute_names_set |= set(edge_attr_dict.keys())
            edge_export_list.append(edge_attr_dict)
        export_file_path = export_folder_path + '/edge_' + edge_label + '.csv'
        edge_attribute_names_set = sorted(edge_attribute_names_set, key=str.lower)
        write_dict_csv(export_file_path, edge_attribute_names_set, edge_export_list)
    print('Completed exporting Edges\n')

    
def export_vertices(graph_traversal, export_folder_path, labels_filter_list=[]):
    print('Exporting Vertices')
    all_vertex_labels_in_database = graph_traversal.V().label().dedup().toList()
    if len(labels_filter_list) == 0:
        vertex_label_list = all_vertex_labels_in_database
    else:
        vertex_label_list = remove_non_exsistent_labels(labels_filter_list, all_vertex_labels_in_database)
    vertex_label_list.sort(key=str.lower)
    for vertex_label in vertex_label_list:
        print('Exporting Vertex with Label:', vertex_label)
        vertices_id_list = gremlin_traversal.V().hasLabel(vertex_label).id().toList()
        vertex_attribute_names_set = set()
        vertex_export_list = []
        for vertex_id in vertices_id_list:
            vertex_attr_dict = graph_traversal.V(vertex_id).valueMap().next()
            for attr_key in vertex_attr_dict.keys():
                vertex_attr_dict[attr_key] = ','.join(map(str, vertex_attr_dict[attr_key]))
            vertex_attr_dict['~id'] = vertex_id
            vertex_attr_dict['~label'] = vertex_label
            vertex_attribute_names_set |= set(vertex_attr_dict.keys())
            vertex_export_list.append(vertex_attr_dict)
        export_file_path = export_folder_path + '/vertex_' + vertex_label + '.csv'
        vertex_attribute_names_set = sorted(vertex_attribute_names_set, key=str.lower)
        write_dict_csv(export_file_path, vertex_attribute_names_set, vertex_export_list)
    print ("Completed exporting Vertices\n")


def create_dirs(folder_path):
    if not os.path.exists(folder_path):
        os.makedirs(folder_path)


if __name__ == "__main__":
    sys.setrecursionlimit(10000)
    # Verify args
    if len(sys.argv) != 3:
        print ('Usage: python export_to_csv.py <neptune-endpoint> <neptune-port>')
        sys.exit(1)

    neptune_endpoint = sys.argv[1]
    try:
        neptune_port = int(sys.argv[2])
    except ValueError:
        print("Please enter a valid port number e.g. 8182")
        sys.exit(1)

    # Create a connection to Neptune and export data
    try:
        gremlin_traversal = gremlin_interface.get_gremlin_connection(neptune_endpoint, neptune_port)
        export_folder_path = os.getcwd() + '/csv'
        create_dirs(export_folder_path)
        print('CSV files will be exported to "current_directory/csv/"\n')
        export_vertices(gremlin_traversal, export_folder_path)
        export_edges(gremlin_traversal, export_folder_path)
    except gaierror:
        print("Could not establish connection with the Neptune Server. Invalid Connection Parameters")
        sys.exit(1)
    print('Export Complete. CSV files are exported to "current_directory/csv/"')
    
   
    
    