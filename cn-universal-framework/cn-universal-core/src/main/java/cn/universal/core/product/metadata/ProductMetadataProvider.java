package cn.universal.core.product.metadata;

/** Supplies protocol-owned metadata used when a product is created. */
public interface ProductMetadataProvider {

  String getProtocolCode();

  String getMetadata();
}
