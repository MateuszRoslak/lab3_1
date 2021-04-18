package pl.com.bottega.ecommerce.sales.domain.invoicing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.ClientData;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.Id;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductData;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductDataBuilder;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductType;
import pl.com.bottega.ecommerce.sharedkernel.Money;

@ExtendWith(MockitoExtension.class)
class BookKeeperTest {

    private static final Id EXAMPLE_CLIENT_ID = Id.generate();
    private static final String EXAMPLE_CLIENT_NAME = "Karol Nowak";
    private static final ClientData EXAMPLE_CLIENT_DATA = new ClientData(EXAMPLE_CLIENT_ID, EXAMPLE_CLIENT_NAME);
    private static final Tax EXAMPLE_TAX = new Tax(Money.ZERO, "example tax name");
    private static final Id EXAMPLE_INVOICE_ID = Id.generate();
    private BookKeeper bookKeeper;

    @Mock
    private TaxPolicy taxPolicyMock;

    @Mock
    private InvoiceFactory invoiceFactoryMock;

    @BeforeEach
    void setUp() throws Exception {
        bookKeeper = new BookKeeper(invoiceFactoryMock);
    }

    @Test
    public void shouldReturnOneItemInvoice() {
        int expectedItems = 1;

        InvoiceRequest request = new InvoiceRequest(EXAMPLE_CLIENT_DATA);

        ProductData productData = new ProductDataBuilder().withProductId(Id.generate())
                .withPrice(Money.ZERO)
                .withName("example product name")
                .withProductType(ProductType.STANDARD)
                .withSnapshotDate(null)
                .build();

        RequestItem requestItem = new RequestItem(productData, 1, Money.ZERO);
        request.add(requestItem);

        when(taxPolicyMock.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(EXAMPLE_TAX);

        Invoice sampleInvoice = new Invoice(EXAMPLE_INVOICE_ID, EXAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(EXAMPLE_CLIENT_DATA)).thenReturn(sampleInvoice);

        Invoice invoice = bookKeeper.issuance(request, taxPolicyMock);

        assertEquals(expectedItems, invoice.getItems().size());
    }

    @Test
    public void shouldInvokeCalculateTaxTwoTimes() {
        int expectedInvokes = 2;

        InvoiceRequest request = new InvoiceRequest(EXAMPLE_CLIENT_DATA);

        ProductData productData1 = new ProductDataBuilder().withProductId(Id.generate())
                .withPrice(Money.ZERO)
                .withName("example product name")
                .withProductType(ProductType.STANDARD)
                .withSnapshotDate(null)
                .build();

        Money requestItem1Cost = new Money(11, Money.DEFAULT_CURRENCY);
        RequestItem requestItem1= new RequestItem(productData1, 1, requestItem1Cost);

        ProductData productData2 = new ProductDataBuilder().withProductId(Id.generate())
                .withPrice(Money.ZERO)
                .withName("example product name")
                .withProductType(ProductType.DRUG)
                .withSnapshotDate(null)
                .build();

        Money requestItem2Cost = new Money(20, Money.DEFAULT_CURRENCY);
        RequestItem requestItem2 = new RequestItem(productData2, 2, requestItem2Cost);

        request.add(requestItem1);
        request.add(requestItem2);

        when(taxPolicyMock.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(EXAMPLE_TAX);

        Invoice invoice = new Invoice(EXAMPLE_INVOICE_ID, EXAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(EXAMPLE_CLIENT_DATA)).thenReturn(invoice);

        ArgumentCaptor<ProductType> productTypeCaptor = ArgumentCaptor.forClass(ProductType.class);
        ArgumentCaptor<Money> moneyCaptor = ArgumentCaptor.forClass(Money.class);

        bookKeeper.issuance(request, taxPolicyMock);

        verify(taxPolicyMock, times(expectedInvokes)).calculateTax(productTypeCaptor.capture(), moneyCaptor.capture());

        assertEquals(productData1.getType(), productTypeCaptor.getAllValues().get(0));
        assertEquals(requestItem1Cost, moneyCaptor.getAllValues().get(0));

        assertEquals(productData2.getType(), productTypeCaptor.getAllValues().get(1));
        assertEquals(requestItem2Cost, moneyCaptor.getAllValues().get(1));
    }

    @Test
    public void shouldReturnEmptyInvoice() {
        int expectedItems = 0;

        InvoiceRequest request = new InvoiceRequest(EXAMPLE_CLIENT_DATA);

        Invoice exampleInvoice = new Invoice(EXAMPLE_INVOICE_ID, EXAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(EXAMPLE_CLIENT_DATA)).thenReturn(exampleInvoice);

        Invoice invoice = bookKeeper.issuance(request, taxPolicyMock);

        assertEquals(expectedItems, invoice.getItems().size());
    }

    @Test
    public void shouldReturnInvoiceWithItemCorrespondingToRequestItem() {
        int requestItems = 1;

        InvoiceRequest request = new InvoiceRequest(EXAMPLE_CLIENT_DATA);

        ProductData productData = new ProductDataBuilder().withProductId(Id.generate())
                .withPrice(Money.ZERO)
                .withName("example product name")
                .withProductType(ProductType.STANDARD)
                .withSnapshotDate(null)
                .build();

        Money requestItemTotalCost = new Money(21, Money.DEFAULT_CURRENCY);
        RequestItem requestItemDummy = new RequestItem(productData, requestItems, requestItemTotalCost);
        request.add(requestItemDummy);

        when(taxPolicyMock.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(EXAMPLE_TAX);

        Invoice exampleInvoice = new Invoice(EXAMPLE_INVOICE_ID, EXAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(EXAMPLE_CLIENT_DATA)).thenReturn(exampleInvoice);

        Invoice invoice = bookKeeper.issuance(request, taxPolicyMock);

        assertEquals(productData, invoice.getItems().get(0).getProduct());
        assertEquals(requestItems, invoice.getItems().get(0).getQuantity());
        assertEquals(requestItemTotalCost, invoice.getItems().get(0).getNet());
        assertEquals(EXAMPLE_TAX, invoice.getItems().get(0).getTax());
    }

    @Test
    public void shouldNotInteractWithTaxPolicyObject() {
        InvoiceRequest request = new InvoiceRequest(EXAMPLE_CLIENT_DATA);

        Invoice exampleInvoice = new Invoice(EXAMPLE_INVOICE_ID, EXAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(EXAMPLE_CLIENT_DATA)).thenReturn(exampleInvoice);

        bookKeeper.issuance(request, taxPolicyMock);

        verifyNoInteractions(taxPolicyMock);
    }

    @Test
    public void shouldInvokeInvoiceFactory() {
        InvoiceRequest request = new InvoiceRequest(EXAMPLE_CLIENT_DATA);

        ArgumentCaptor<ClientData> clientDataCaptor = ArgumentCaptor.forClass(ClientData.class);

        bookKeeper.issuance(request, taxPolicyMock);

        verify(invoiceFactoryMock, times(1)).create(clientDataCaptor.capture());

        assertEquals(EXAMPLE_CLIENT_DATA, clientDataCaptor.getValue());
    }
}
