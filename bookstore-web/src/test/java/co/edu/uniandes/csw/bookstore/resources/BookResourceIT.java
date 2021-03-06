package co.edu.uniandes.csw.bookstore.resources;

import co.edu.uniandes.csw.bookstore.adapters.DateAdapter;
import co.edu.uniandes.csw.bookstore.converters.BookConverter;
import co.edu.uniandes.csw.bookstore.dtos.AuthorDTO;
import co.edu.uniandes.csw.bookstore.dtos.BookDTO;
import co.edu.uniandes.csw.bookstore.dtos.ReviewDTO;
import co.edu.uniandes.csw.bookstore.entities.BookEntity;
import co.edu.uniandes.csw.bookstore.mappers.EJBExceptionMapper;
import co.edu.uniandes.csw.bookstore.providers.CreatedFilter;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(Arquillian.class)
public class BookResourceIT {

    private final int OK = Status.OK.getStatusCode();
    private final int CREATED = Status.CREATED.getStatusCode();
    private final int NO_CONTENT = Status.NO_CONTENT.getStatusCode();

    private final String bookPath = "books";
    private final String authorsPath = "authors";

    private final static List<BookDTO> oraculo = new ArrayList<>();
    private final static List<AuthorDTO> oraculoAuthors = new ArrayList<>();

    private WebTarget target;
    private final String apiPath = "api";
    private static PodamFactory factory = new PodamFactoryImpl();

    @ArquillianResource
    private URL deploymentURL;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                // Se agrega la dependencia a la logica con el nombre groupid:artefactid:version (GAV)
                .addAsLibraries(Maven.resolver()
                        .resolve("co.edu.uniandes.csw.bookstore:bookstore-logic:1.0-SNAPSHOT")
                        .withTransitivity().asFile())
                // Se agregan los compilados de los paquetes de servicios
                .addPackage(BookResource.class.getPackage())
                .addPackage(BookDTO.class.getPackage())
                .addPackage(BookConverter.class.getPackage())
                .addPackage(EJBExceptionMapper.class.getPackage())
                .addPackage(DateAdapter.class.getPackage())
                .addPackage(CreatedFilter.class.getPackage())
                // El archivo que contiene la configuracion a la base de datos.
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                // El archivo beans.xml es necesario para injeccion de dependencias.
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"))
                // El archivo web.xml es necesario para el despliegue de los servlets
                .setWebXML(new File("src/main/webapp/WEB-INF/web.xml"));
    }

    @Before
    public void setUpTest() {
        target = ClientBuilder.newClient().target(deploymentURL.toString()).path(apiPath);
    }

    @BeforeClass
    public static void setUp() {
        insertData();
    }

    public static void insertData() {
        for (int i = 0; i < 5; i++) {
            BookDTO book = factory.manufacturePojo(BookDTO.class);
            book.setPublishDate(getMaxDate());
            book.setId(i + 1L);
            List<ReviewDTO> reviewsList = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                ReviewDTO reviews = factory.manufacturePojo(ReviewDTO.class);
                reviews.setId(i + 1L);
                reviewsList.add(reviews);
            }

            book.setReviews(reviewsList);

            oraculo.add(book);

            AuthorDTO authors = factory.manufacturePojo(AuthorDTO.class);
            authors.setId(i + 1L);
            oraculoAuthors.add(authors);
        }
    }

    @Test
    @InSequence(1)
    public void createBookTest() {
        BookDTO book = oraculo.get(0);
        Response response = target.path(bookPath).request()
                .post(Entity.entity(book, MediaType.APPLICATION_JSON));

        BookDTO bookTest = (BookDTO) response.readEntity(BookDTO.class);

        Assert.assertEquals(CREATED, response.getStatus());

        Assert.assertEquals(book.getName(), bookTest.getName());
        Assert.assertEquals(book.getDescription(), bookTest.getDescription());
        Assert.assertEquals(book.getIsbn(), bookTest.getIsbn());
        Assert.assertEquals(book.getImage(), bookTest.getImage());
    }

    @Test
    @InSequence(2)
    public void getBookById() {
        BookDTO bookTest = target.path(bookPath)
                .path(oraculo.get(0).getId().toString())
                .request().get(BookDTO.class);
        Assert.assertEquals(bookTest.getId(), oraculo.get(0).getId());
        Assert.assertEquals(bookTest.getName(), oraculo.get(0).getName());
        Assert.assertEquals(bookTest.getDescription(), oraculo.get(0).getDescription());
        Assert.assertEquals(bookTest.getIsbn(), oraculo.get(0).getIsbn());
        Assert.assertEquals(bookTest.getImage(), oraculo.get(0).getImage());
    }

    @Test
    @InSequence(3)
    public void listBookTest() {
        Response response = target.path(bookPath)
                .request().get();

        List<BookDTO> listBookTest = response.readEntity(new GenericType<List<BookDTO>>() {
        });
        Assert.assertEquals(OK, response.getStatus());
        Assert.assertEquals(1, listBookTest.size());
    }

    @Test
    @InSequence(4)
    public void updateBookTest() {
        BookDTO book = oraculo.get(0);
        BookDTO bookChanged = factory.manufacturePojo(BookDTO.class);
        bookChanged.setPublishDate(getMaxDate());
        book.setName(bookChanged.getName());
        book.setDescription(bookChanged.getDescription());
        book.setIsbn(bookChanged.getIsbn());
        book.setImage(bookChanged.getImage());
        book.setPublishDate(bookChanged.getPublishDate());
        Response response = target.path(bookPath).path(book.getId().toString())
                .request().put(Entity.entity(book, MediaType.APPLICATION_JSON));
        BookDTO bookTest = (BookDTO) response.readEntity(BookDTO.class);
        Assert.assertEquals(OK, response.getStatus());
        Assert.assertEquals(book.getName(), bookTest.getName());
        Assert.assertEquals(book.getDescription(), bookTest.getDescription());
        Assert.assertEquals(book.getIsbn(), bookTest.getIsbn());
        Assert.assertEquals(book.getImage(), bookTest.getImage());
    }

    @Test
    @InSequence(9)
    public void deleteBookTest() {
        BookDTO book = oraculo.get(0);
        Response response = target.path(bookPath).path(book.getId().toString())
                .request().delete();
        Assert.assertEquals(NO_CONTENT, response.getStatus());
    }

    @Test
    @InSequence(5)
    public void addAuthorsTest() {

        AuthorDTO authors = oraculoAuthors.get(0);
        BookDTO book = oraculo.get(0);

        Response response = target.path("authors").request()
                .post(Entity.entity(authors, MediaType.APPLICATION_JSON));

        AuthorDTO authorsTest = (AuthorDTO) response.readEntity(AuthorDTO.class);
        Assert.assertEquals(authors.getId(), authorsTest.getId());
        Assert.assertEquals(authors.getName(), authorsTest.getName());
        Assert.assertEquals(authors.getBirthDate(), authorsTest.getBirthDate());
        Assert.assertEquals(CREATED, response.getStatus());

        response = target.path(bookPath).path(book.getId().toString())
                .path(authorsPath).path(authors.getId().toString()).request()
                .post(Entity.entity(authors, MediaType.APPLICATION_JSON));

        authorsTest = (AuthorDTO) response.readEntity(AuthorDTO.class);
        Assert.assertEquals(OK, response.getStatus());
        Assert.assertEquals(authors.getId(), authorsTest.getId());
    }

    @Test
    @InSequence(6)
    public void listAuthorsTest() {
        BookDTO book = oraculo.get(0);

        Response response = target.path(bookPath)
                .path(book.getId().toString())
                .path(authorsPath)
                .request().get();

        List<AuthorDTO> authorsListTest = response.readEntity(new GenericType<List<AuthorDTO>>() {
        });
        Assert.assertEquals(OK, response.getStatus());
        Assert.assertEquals(1, authorsListTest.size());
    }

    @Test
    @InSequence(7)
    public void getAuthorsTest() {
        AuthorDTO authors = oraculoAuthors.get(0);
        BookDTO book = oraculo.get(0);

        AuthorDTO authorsTest = target.path(bookPath)
                .path(book.getId().toString()).path(authorsPath)
                .path(authors.getId().toString())
                .request().get(AuthorDTO.class);

        Assert.assertEquals(authors.getId(), authorsTest.getId());
        Assert.assertEquals(authors.getName(), authorsTest.getName());
        Assert.assertEquals(authors.getBirthDate(), authorsTest.getBirthDate());
    }

    @Test
    @InSequence(8)
    public void removeAuthorsTest() {
        AuthorDTO authors = oraculoAuthors.get(0);
        BookDTO book = oraculo.get(0);

        Response response = target.path(bookPath).path(book.getId().toString())
                .path(authorsPath).path(authors.getId().toString())
                .request().delete();
        Assert.assertEquals(NO_CONTENT, response.getStatus());
    }

    private static Date getMaxDate() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 9999);
        c.set(Calendar.DAY_OF_YEAR, c.getActualMaximum(Calendar.DAY_OF_YEAR));
        c.set(Calendar.HOUR_OF_DAY, c.getActualMinimum(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, c.getActualMinimum(Calendar.MINUTE));
        c.set(Calendar.SECOND, c.getActualMinimum(Calendar.SECOND));
        c.set(Calendar.MILLISECOND, c.getActualMinimum(Calendar.MILLISECOND));
        return c.getTime();
    }
}
