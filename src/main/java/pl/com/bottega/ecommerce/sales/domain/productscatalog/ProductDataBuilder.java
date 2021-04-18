package pl.com.bottega.ecommerce.sales.domain.productscatalog;

import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.Id;
import pl.com.bottega.ecommerce.sharedkernel.Money;

import java.util.Date;

public class ProductDataBuilder {

    Id productId;
    Money price;
    String name;
    Date snapshotDate;
    ProductType type;

    public ProductDataBuilder() {}

    public ProductDataBuilder withProductId(Id productId) {
        this.productId = productId;
        return this;
    }

    public ProductDataBuilder withPrice(Money price) {
        this.price = price;
        return this;
    }

    public ProductDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ProductDataBuilder withSnapshotDate(Date snapshotDate) {
        this.snapshotDate = snapshotDate;
        return this;
    }

    public ProductDataBuilder withProductType(ProductType type) {
        this.type = type;
        return this;
    }

    public ProductData build() {
        return new ProductData(productId, price, name, type, snapshotDate);
    }
}
