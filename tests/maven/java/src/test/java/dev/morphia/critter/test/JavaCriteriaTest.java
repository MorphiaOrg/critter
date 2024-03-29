package dev.morphia.critter.test;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.Morphia;
import dev.morphia.UpdateOptions;
import dev.morphia.critter.test.criteria.InvoiceCriteria;
import dev.morphia.critter.test.criteria.PersonCriteria;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import org.testcontainers.containers.MongoDBContainer;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.WriteConcern.MAJORITY;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.or;
import static java.lang.String.format;
import static org.bson.UuidRepresentation.STANDARD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@SuppressWarnings("removal")
@Test
public class JavaCriteriaTest {
    private final String STANDARD_CODECS = "Standard codecs";
    private final String CRITTER_CODECS = "Critter codecs";

    private MongoDBContainer mongoDBContainer;
    private MongoClient mongoClient;
    private Datastore datastore;

    @BeforeTest
    void setup() {
        mongoDBContainer = new MongoDBContainer("mongo:6.0.4");
        mongoDBContainer.start();

        mongoClient = MongoClients.create(
            MongoClientSettings.builder()
                               .uuidRepresentation(STANDARD)
                               .applyConnectionString(new ConnectionString(mongoDBContainer.getReplicaSetUrl()))
                               .build()
                                         );
    }

    @AfterTest
    void shutdown() {
        if (mongoDBContainer != null ) {
            mongoDBContainer.close();
        }
    }

    @Test(dataProvider = "datastores")
    public void parents(String state, Datastore datastore) {
        if (state == STANDARD_CODECS) {
            datastore.getMapper().map(
                RootParent.class,
                ChildLevel1a.class,
                ChildLevel1b.class,
                ChildLevel1c.class,
                ChildLevel2a.class,
                ChildLevel2b.class,
                ChildLevel3a.class,
                TestEntity.class);
        }

        checkSubtypes(datastore, RootParent.class, 6);
        checkSubtypes(datastore, TestEntity.class, 7);
    }

    private static void checkSubtypes(Datastore datastore, Class<?> type, int expected) {
        Set<EntityModel> subtypes = datastore.getMapper().getEntityModel(type).getSubtypes();
        assertEquals(subtypes.size(), expected, format("Expected %d subtypes: %s", expected,
            subtypes.stream().map(EntityModel::getName).collect(Collectors.toList())));
    }

    @Test(dataProvider = "datastores")
    public void andQueries(String state, Datastore datastore) {
        datastore.save(new Person("Mike", "Bloomberg"));
        datastore.save(new Person("Mike", "Tyson"));

        final Query<Person> query1 = datastore.find(Person.class)
                                              .filter(Filters.and(
                                                  PersonCriteria.firstName().eq("Mike"),
                                                  PersonCriteria.lastName().eq("Tyson")));

        final Query<Person> query2 = datastore.find(Person.class)
                                              .filter(
                                                  eq("firstName", "Mike"),
                                                  eq("lastName", "Tyson"));
        assertEquals(query2.iterator().toList().size(), 1);
        assertEquals(query1.iterator().toList(), query2.iterator().toList());
    }

    @BeforeMethod
    public void clean() {
        datastore.getDatabase().drop();
    }

    @Test(dataProvider = "datastores")
    public void embeds(String state, Datastore datastore) {
        Person person = new Person("Mike", "Bloomberg");
        datastore.save(person);
        Invoice invoice = new Invoice(LocalDateTime.now(), person, new Address("New York City", "NY", "10036"));
        datastore.save(invoice);

        person = new Person("Andy", "Warhol");
        datastore.save(person);

        invoice = new Invoice(LocalDateTime.now(), person, new Address("NYC", "NY", "10018"));

        datastore.save(invoice);

        MorphiaCursor<Invoice> criteria1 = datastore.find(Invoice.class)
                                                    .filter(InvoiceCriteria.orderDate().lte(LocalDateTime.now().plusDays(5)))
                                                    .iterator(new FindOptions()
                                                                  .sort(ascending(InvoiceCriteria.addresses().city().path())));
        List<Invoice> list = criteria1.toList();
        assertEquals(list.get(0).getAddresses().get(0).getCity(), "NYC", list.stream().map(Invoice::getId).collect(
            Collectors.toList()).toString());
        assertEquals(list.get(0), invoice, list.stream().map(Invoice::getId).collect(Collectors.toList()).toString());

        MorphiaCursor<Invoice> criteria2 = datastore.find(Invoice.class)
                                                    .iterator(new FindOptions()
                                                                  .sort(descending(InvoiceCriteria.addresses().city().path())));
        assertEquals(criteria2.toList().get(0).getAddresses().get(0).getCity(), "New York City");
    }

    @Test(dataProvider = "datastores")
    public void invoice(String state, Datastore ds) {
        assertTrue(!ds.getMapper().getOptions().autoImportModels() ^ ds.getMapper().isMapped(Invoice.class));
        Person john = new Person("John", "Doe");
        ds.save(john);
        ds.save(new Invoice(LocalDateTime.of(2012, 12, 21, 13, 15), john,
            new Address("New York City", "NY", "10000"),
            new Item("ball", 5.0), new Item("skateboard", 17.35)));
        Person jeff = new Person("Jeff", "Johnson");
        ds.save(jeff);
        ds.save(new Invoice(LocalDateTime.of(2006, 3, 4, 8, 7), jeff, new Address("Los Angeles", "CA", "90210"),
            new Item("movie", 29.95)));
        Person sally = new Person("Sally", "Ride");
        ds.save(sally);
        ds.save(new Invoice(LocalDateTime.of(2007, 8, 16, 19, 27), sally, new Address("Chicago", "IL", "99999"),
            new Item("kleenex", 3.49), new Item("cough and cold syrup", 5.61)));
        Query<Invoice> query = ds.find(Invoice.class)
                                 .filter(InvoiceCriteria.person().eq(john));
        Invoice invoice = query.first();
        Invoice doe = ds.find(Invoice.class)
                        .filter(eq("person", john))
                        .first();

        assertNotNull(doe);
        assertEquals(invoice, doe);
        assertEquals(doe.getPerson().getLastName(), "Doe");

        assertNotNull(invoice);
        assertNotNull(invoice.getPerson());
        assertEquals(invoice.getPerson().getLastName(), "Doe");
        invoice = ds.find(Invoice.class)
                    .filter(eq("addresses.city", "Chicago"))
                    .first();
        assertNotNull(invoice);
        Invoice critter = ds.find(Invoice.class)
                            .filter(InvoiceCriteria.addresses().city().eq("Chicago"))
                            .first();
        assertNotNull(critter);
        assertEquals(critter, invoice);

        Invoice created = new Invoice(LocalDateTime.of(2012, 12, 21, 13, 15), john,
            List.of(new Address("New York City", "NY", "10000")),
            List.of(new Item("ball", 5.0), new Item("skateboard", 17.35)));
        ds.save(created);
        assertTrue(created.isPrePersist());
        assertTrue(created.isPostPersist());
        assertFalse(created.isPreLoad());
        assertFalse(created.isPostLoad());

        Invoice loaded = ds.find(Invoice.class).filter(eq(InvoiceCriteria.id, created.getId())).first();
        assertFalse(loaded.isPrePersist());
        assertFalse(loaded.isPostPersist());
        assertTrue(loaded.isPreLoad());
        assertTrue(loaded.isPostLoad());
    }

    @Test(dataProvider = "datastores")
    public void orQueries(String state, Datastore datastore) {
        datastore.save(new Person("Mike", "Bloomberg"));
        datastore.save(new Person("Mike", "Tyson"));

        final Query<Person> query = datastore.find(Person.class)
                                             .filter(or(
                                                 eq("lastName", "Bloomberg"),
                                                 eq("lastName", "Tyson")));

        final Query<Person> criteria = datastore.find(Person.class)
                                                .filter(or(
                                                    PersonCriteria.lastName().eq("Bloomberg"),
                                                    PersonCriteria.lastName().eq("Tyson")));

        assertEquals(criteria.count(), 2);
        assertEquals(query.iterator().toList(), criteria.iterator().toList());
    }

    public void paths() {
        assertEquals(InvoiceCriteria.addresses().city().path(), "addresses.city");
        assertEquals(InvoiceCriteria.orderDate().path(), "orderDate");
    }

    @Test(dataProvider = "datastores")
    public void removes(String state, Datastore datastore) {
        for (int i = 0; i < 100; i++) {
            datastore.save(new Person("First" + i, "Last" + i));
        }
        Query<Person> criteria = datastore.find(Person.class)
                                          .filter(PersonCriteria.lastName().regex()
                                                                .pattern("Last2"));
        DeleteResult result = criteria.delete(new DeleteOptions()
                                                  .multi(true));
        assertEquals(result.getDeletedCount(), 11);
        assertEquals(criteria.count(), 0);

        criteria = datastore.find(Person.class);
        assertEquals(criteria.count(), 89);

        criteria = datastore.find(Person.class)
                            .filter(PersonCriteria.lastName().regex()
                                                  .pattern("Last3"));
        result = criteria.delete(new DeleteOptions()
                                     .multi(true)
                                     .writeConcern(MAJORITY));
        assertEquals(result.getDeletedCount(), 11);

        assertEquals(criteria.count(), 0);
    }

    @Test(dataProvider = "datastores")
    public void updateFirst(String state, Datastore datastore) {
        for (int i = 0; i < 100; i++) {
            datastore.save(new Person("First" + i, "Last" + i));
        }
        Query<Person> query = datastore.find(Person.class)
                                       .filter(PersonCriteria.lastName().regex()
                                                             .pattern("Last2"));

        query.update(PersonCriteria.age().set(1000L))
             .execute();

        query = datastore.find(Person.class)
                         .filter(PersonCriteria.age().eq(1000L));

        assertEquals(query.count(), 1L);
    }

    @Test(dataProvider = "datastores")
    public void updates(String state, Datastore datastore) {
        Query<Person> query = datastore.find(Person.class);
        query.delete();

        query.filter(
            PersonCriteria.firstName().eq("Jim"),
            PersonCriteria.lastName().eq("Beam"));

        assertEquals(query.update(
                              PersonCriteria.age().set(30L))
                          .execute(new UpdateOptions().multi(true))
                          .getModifiedCount(), 0);

        assertNotNull(query.update(PersonCriteria.age().set(30L))
                           .execute(new UpdateOptions().upsert(true))
                           .getUpsertedId());

        final UpdateResult update = query.update(PersonCriteria.age().inc())
                                         .execute(new UpdateOptions().multi(true));
        assertEquals(update.getModifiedCount(), 1);
        assertEquals(datastore.find(Person.class)
                              .first().getAge().longValue(), 31L);

        assertNotNull(datastore.find(Person.class).first().getFirstName());

        DeleteResult delete = datastore.find(Person.class).delete();
        assertEquals(delete.getDeletedCount(), 1);
    }

    @DataProvider(name = "datastores")
    private Object[][] datastores() {
        return new Object[][]{
            new Object[]{"Standard codecs", getDatastore(false)},
            new Object[]{"Critter codecs", getDatastore(true)},
            };
    }

    private Datastore getDatastore(boolean useGenerated) {
        return datastore = Morphia.createDatastore(mongoClient, "test",
            MapperOptions.builder()
                         .autoImportModels(useGenerated)
                         .build());
    }


}
