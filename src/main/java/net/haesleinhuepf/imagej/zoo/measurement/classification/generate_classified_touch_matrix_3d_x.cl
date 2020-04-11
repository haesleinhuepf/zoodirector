
__kernel void generate_classified_touch_matrix_3d(
    IMAGE_dst_matrix_TYPE dst_matrix,
    IMAGE_src_classification_TYPE src_classification,
    IMAGE_src_label_map_TYPE src_label_map

) {
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

  const int x = get_global_id(0);
  const int y = get_global_id(1);
  const int z = get_global_id(2);

  if (x > GET_IMAGE_WIDTH(src_label_map) - 2) {
    return;
  }
  if (y > GET_IMAGE_HEIGHT(src_label_map) - 2) {
    return;
  }
  if (z > GET_IMAGE_DEPTH(src_label_map) - 2) {
    return;
  }

  float label = READ_IMAGE(src_label_map, sampler, POS_src_label_map_INSTANCE(x, y, z, 0)).x;
  float labelx = READ_IMAGE(src_label_map, sampler, POS_src_label_map_INSTANCE(x + 1, y, z, 0)).x;
  float labely = READ_IMAGE(src_label_map, sampler, POS_src_label_map_INSTANCE(x, y + 1, z, 0)).x;
  float labelz = READ_IMAGE(src_label_map, sampler, POS_src_label_map_INSTANCE(x, y, z + 1, 0)).x;

  float class = READ_IMAGE(src_classification, sampler, POS_src_classification_INSTANCE(label, 0, 0, 0)).x;
  float classx = READ_IMAGE(src_classification, sampler, POS_src_classification_INSTANCE(labelx, 0, 0, 0)).x;
  float classy = READ_IMAGE(src_classification, sampler, POS_src_classification_INSTANCE(labely, 0, 0, 0)).x;
  float classz = READ_IMAGE(src_classification, sampler, POS_src_classification_INSTANCE(labelz, 0, 0, 0)).x;

  if (label < labelx && class == classx) {
    WRITE_IMAGE(dst_matrix, (POS_dst_matrix_INSTANCE(label, labelx, 0, 0)), CONVERT_dst_matrix_PIXEL_TYPE(class));
  } else if (label > labelx && class == classx) {
    WRITE_IMAGE(dst_matrix, (POS_dst_matrix_INSTANCE(labelx, label, 0, 0)), CONVERT_dst_matrix_PIXEL_TYPE(class));
  }
  if (label < labely && class == classy) {
    WRITE_IMAGE(dst_matrix, (POS_dst_matrix_INSTANCE(label, labely, 0, 0)), CONVERT_dst_matrix_PIXEL_TYPE(class));
  } else if (label > labely && class == classy) {
    WRITE_IMAGE(dst_matrix, (POS_dst_matrix_INSTANCE(labely, label, 0, 0)), CONVERT_dst_matrix_PIXEL_TYPE(class));
  }
  if (label < labelz && class == classz) {
    WRITE_IMAGE(dst_matrix, (POS_dst_matrix_INSTANCE(label, labelz, 0, 0)), CONVERT_dst_matrix_PIXEL_TYPE(class));
  } else if (label > labelz && class == classz) {
    WRITE_IMAGE(dst_matrix, (POS_dst_matrix_INSTANCE(labelz, label, 0, 0)), CONVERT_dst_matrix_PIXEL_TYPE(class));
  }
}
