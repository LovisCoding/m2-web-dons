package fr.donner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:donner-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "donner.upload-dir=./target/test-uploads"
})
@AutoConfigureMockMvc
class WebIntegrationTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rendersListingsSupportsContentNegotiationPaginationAndConditionalRequests() throws Exception {
        // HTML sans en-tête Accept
        mvc.perform(get("/annonces"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("class=\"card-photo\"")));

        // HTML avec en-tête Accept navigateur réaliste (contient */*)
        mvc.perform(get("/annonces")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mvc.perform(get("/annonces/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("class=\"detail-photo\"")));

        // Variantes avec barre oblique finale
        mvc.perform(get("/annonces/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mvc.perform(get("/annonces/1/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        // Négociation de contenu HAL/JSON sur les routes web
        mvc.perform(get("/annonces").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.html.href").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(102)));

        mvc.perform(get("/annonces/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.imageUrl", notNullValue()))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/annonces/1")))
                .andExpect(jsonPath("$._links.html.href", containsString("/annonces/1")));

        // ETag, Cache-Control et If-None-Match (304)
        MvcResult apiResult = mvc.perform(get("/api/annonces").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", containsString("max-age=30")))
                .andReturn();
        String etag = apiResult.getResponse().getHeader("ETag");

        mvc.perform(get("/api/annonces").accept(MediaTypes.HAL_JSON).header("If-None-Match", etag))
                .andExpect(status().isNotModified());

        // Pagination & tri
        mvc.perform(get("/api/annonces?page=0&size=1&sort=title,asc").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.last.href").exists());

        mvc.perform(get("/api/annonces?page=1&size=1&sort=title,asc").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$._links.prev.href").exists());

        mvc.perform(get("/annonces?page=0&size=1&sort=area,asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void enforcesAuthenticationAndAllowsRegistration() throws Exception {
        // Endpoints protégés rejettent l'accès anonyme
        mvc.perform(post("/api/annonces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Chaise\",\"state\":\"BON\",\"area\":\"Lyon\",\"deliveryMode\":\"MAIN_PROPRE\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":1,\"body\":\"Bonjour\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/personnes/1/favoris/1"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/personnes/1/favoris"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/annonces/nouvelle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mvc.perform(get("/mon-espace"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Inscription API puis authentification HTTP Basic
        String email = "claire-" + System.nanoTime() + "@example.fr";
        mvc.perform(post("/api/personnes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Claire\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/personnes/")))
                .andExpect(jsonPath("$._links.favoris.href").exists());

        // Création d'une annonce avec le nouveau compte
        MvcResult created = mvc.perform(post("/api/annonces")
                        .with(httpBasic(email, "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bureau Claire\",\"description\":\"Solide\",\"state\":\"BON\",\"area\":\"Paris\",\"deliveryMode\":\"MAIN_PROPRE\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long listingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Nettoyage
        mvc.perform(delete("/api/annonces/" + listingId).with(httpBasic(email, "secret123")))
                .andExpect(status().isNoContent());

        // Inscription HTML
        String htmlEmail = "luc-" + System.nanoTime() + "@example.fr";
        mvc.perform(post("/inscription")
                        .with(csrf())
                        .param("name", "Luc")
                        .param("email", htmlEmail)
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login*"));
    }

    @Test
    void listingWriteLifecycleAndAuthorization() throws Exception {
        // Création API en tant que Camille
        String chairPhoto = "https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=800&q=80";
        String oakChairPhoto = "https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?auto=format&fit=crop&w=800&q=80";
        MvcResult createResult = mvc.perform(post("/api/annonces")
                        .with(httpBasic("camille@example.fr", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Chaise en bois\",\"description\":\"Bon état\",\"state\":\"BON\",\"area\":\"Lyon\",\"deliveryMode\":\"MAIN_PROPRE\",\"keywords\":[\"meuble\"],\"imageUrl\":\"" + chairPhoto + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Chaise en bois"))
                .andExpect(jsonPath("$.imageUrl").value(chairPhoto))
                .andExpect(jsonPath("$._links.self.href", notNullValue()))
                .andExpect(jsonPath("$._links.proprietaire.href", containsString("/api/personnes/1")))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long listingId = created.get("id").asLong();

        // Modification interdite à un autre utilisateur (Yanis)
        mvc.perform(put("/api/annonces/" + listingId)
                        .with(httpBasic("yanis@example.fr", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Piratée\",\"description\":\"Non\",\"state\":\"BON\",\"area\":\"Lyon\",\"deliveryMode\":\"MAIN_PROPRE\"}"))
                .andExpect(status().isForbidden());

        // Modification autorisée à Camille
        mvc.perform(put("/api/annonces/" + listingId)
                        .with(httpBasic("camille@example.fr", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Chaise en chêne\",\"description\":\"Très bon état\",\"state\":\"TRES_BON\",\"area\":\"Lyon\",\"deliveryMode\":\"LES_DEUX\",\"keywords\":[\"meuble\",\"bois\"],\"imageUrl\":\"" + oakChairPhoto + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Chaise en chêne"))
                .andExpect(jsonPath("$.imageUrl").value(oakChairPhoto));

        // Upload d'une photo via l'API multipart
        MockMultipartFile pngPhoto = new MockMultipartFile(
                "file", "chaise.png", "image/png",
                new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "pas une image".getBytes());

        mvc.perform(multipart("/api/annonces/" + listingId + "/photo")
                        .file(pngPhoto)
                        .with(httpBasic("yanis@example.fr", "password")))
                .andExpect(status().isForbidden());

        mvc.perform(multipart("/api/annonces/" + listingId + "/photo")
                        .file(invalidFile)
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isBadRequest());

        MvcResult apiUpload = mvc.perform(multipart("/api/annonces/" + listingId + "/photo")
                        .file(pngPhoto)
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", startsWith("/uploads/")))
                .andExpect(jsonPath("$._links.photo.href", containsString("/api/annonces/" + listingId + "/photo")))
                .andReturn();
        String uploadedApiUrl = objectMapper.readTree(apiUpload.getResponse().getContentAsString())
                .get("imageUrl").asText();

        mvc.perform(get(uploadedApiUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));

        // Suppression interdite à Yanis
        mvc.perform(delete("/api/annonces/" + listingId)
                        .with(httpBasic("yanis@example.fr", "password")))
                .andExpect(status().isForbidden());

        // Suppression autorisée à Camille
        mvc.perform(delete("/api/annonces/" + listingId)
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/annonces/" + listingId))
                .andExpect(status().isNotFound());

        // Cycle complet via formulaires HTML (création, modification, refus 403, suppression)
        String tablePhoto = "https://images.unsplash.com/photo-1533090481720-856c6e3c1fdc?auto=format&fit=crop&w=800&q=80";
        MvcResult htmlCreate = mvc.perform(post("/annonces")
                        .with(user("camille@example.fr"))
                        .with(csrf())
                        .param("title", "Table HTML")
                        .param("description", "Table ronde")
                        .param("state", "BON")
                        .param("area", "Lyon")
                        .param("deliveryMode", "MAIN_PROPRE")
                        .param("keywords", "meuble, salon")
                        .param("imageUrl", tablePhoto))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = htmlCreate.getResponse().getRedirectedUrl();
        long htmlId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mvc.perform(post("/annonces/" + htmlId + "/modifier")
                        .with(user("yanis@example.fr"))
                        .with(csrf())
                        .param("title", "Volée")
                        .param("description", "Non")
                        .param("state", "BON")
                        .param("area", "Lyon")
                        .param("deliveryMode", "MAIN_PROPRE"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/annonces/" + htmlId + "/modifier")
                        .with(user("camille@example.fr"))
                        .with(csrf())
                        .param("title", "Table HTML modifiée")
                        .param("description", "Table carrée")
                        .param("state", "TRES_BON")
                        .param("area", "Villeurbanne")
                        .param("deliveryMode", "ENVOI")
                        .param("keywords", "meuble")
                        .param("imageUrl", tablePhoto))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/api/annonces/" + htmlId).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Table HTML modifiée"))
                .andExpect(jsonPath("$.imageUrl").value(tablePhoto));

        mvc.perform(post("/annonces/" + htmlId + "/supprimer")
                        .with(user("camille@example.fr"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Création HTML avec upload de fichier image et rejet d'un fichier non-image
        MockMultipartFile htmlInvalidFile = new MockMultipartFile(
                "photo", "doc.txt", "text/plain", "texte".getBytes());
        mvc.perform(multipart("/annonces")
                        .file(htmlInvalidFile)
                        .with(user("camille@example.fr"))
                        .with(csrf())
                        .param("title", "Annonce invalide")
                        .param("description", "Test")
                        .param("state", "BON")
                        .param("area", "Lyon")
                        .param("deliveryMode", "MAIN_PROPRE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Format d’image non pris en charge")));

        MockMultipartFile jpgPhoto = new MockMultipartFile(
                "photo", "table.jpg", "image/jpeg",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 });
        MvcResult htmlUploadCreate = mvc.perform(multipart("/annonces")
                        .file(jpgPhoto)
                        .with(user("camille@example.fr"))
                        .with(csrf())
                        .param("title", "Table avec upload")
                        .param("description", "Photo importée")
                        .param("state", "TRES_BON")
                        .param("area", "Lyon")
                        .param("deliveryMode", "MAIN_PROPRE"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String uploadLocation = htmlUploadCreate.getResponse().getRedirectedUrl();
        long uploadHtmlId = Long.parseLong(uploadLocation.substring(uploadLocation.lastIndexOf('/') + 1));

        MvcResult uploadedDetailJson = mvc.perform(get("/api/annonces/" + uploadHtmlId).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", startsWith("/uploads/")))
                .andReturn();
        String htmlUploadedUrl = objectMapper.readTree(uploadedDetailJson.getResponse().getContentAsString())
                .get("imageUrl").asText();

        mvc.perform(get(htmlUploadedUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        mvc.perform(post("/annonces/" + uploadHtmlId + "/supprimer")
                        .with(user("camille@example.fr"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void favoritesWriteLifecycleAndHalLinks() throws Exception {
        // Yanis (id 2) ajoute l'annonce 1 aux favoris
        mvc.perform(post("/api/personnes/2/favoris/1")
                        .with(httpBasic("yanis@example.fr", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/annonces/1")))
                .andExpect(jsonPath("$._links.favori.href", containsString("/api/personnes/2/favoris/1")));

        // Camille ne peut pas lire ni modifier les favoris de Yanis
        mvc.perform(get("/api/personnes/2/favoris")
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/personnes/2/favoris/1")
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isForbidden());

        // Lecture HAL sur /api/personnes/2/favoris et négociation sur /personnes/2/favoris
        mvc.perform(get("/api/personnes/2/favoris")
                        .with(httpBasic("yanis@example.fr", "password"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/personnes/2/favoris")))
                .andExpect(jsonPath("$._embedded.listingList[0]._links.self.href", containsString("/api/annonces/1")));

        mvc.perform(get("/personnes/2/favoris")
                        .with(httpBasic("yanis@example.fr", "password"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/personnes/2/favoris")));

        // Suppression du favori
        mvc.perform(delete("/api/personnes/2/favoris/1")
                        .with(httpBasic("yanis@example.fr", "password")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/personnes/2/favoris")
                        .with(httpBasic("yanis@example.fr", "password"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    void savedSearchesWriteLifecycleAndHalLinks() throws Exception {
        MvcResult created = mvc.perform(post("/api/personnes/1/recherches")
                        .with(httpBasic("camille@example.fr", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"livre\",\"area\":\"Lyon\",\"state\":\"BON\",\"keyword\":\"culture\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/recherches/")))
                .andExpect(jsonPath("$._links.notifications.href").exists())
                .andReturn();

        long searchId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/personnes/1/recherches")
                        .with(httpBasic("camille@example.fr", "password"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/personnes/1/recherches")))
                .andExpect(jsonPath("$._embedded.savedSearchList[0]._links.self.href").exists());

        mvc.perform(put("/api/recherches/" + searchId)
                        .with(httpBasic("camille@example.fr", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"lampe\",\"area\":\"Villeurbanne\",\"state\":\"TRES_BON\",\"keyword\":\"maison\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("lampe"))
                .andExpect(jsonPath("$.area").value("Villeurbanne"));

        mvc.perform(patch("/api/recherches/" + searchId + "/notifications?enabled=false")
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").value(false));

        // Yanis ne peut pas modifier la recherche de Camille
        mvc.perform(patch("/api/recherches/" + searchId + "/notifications?enabled=true")
                        .with(httpBasic("yanis@example.fr", "password")))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/recherches/" + searchId)
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isNoContent());
    }

    @Test
    void lotsWriteLifecycleAndHalLinks() throws Exception {
        MvcResult created = mvc.perform(post("/api/personnes/1/lots")
                        .with(httpBasic("camille@example.fr", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lot salon\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lot salon"))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/lots/")))
                .andExpect(jsonPath("$._links.proprietaire.href", containsString("/api/personnes/1")))
                .andReturn();

        long lotId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Ajout d'une annonce de Camille (id 1)
        mvc.perform(post("/api/lots/" + lotId + "/annonces/1")
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.annonces.href", containsString("/api/annonces/1")));

        // Rejet de l'ajout d'une annonce appartenant à Yanis (id 2)
        mvc.perform(post("/api/lots/" + lotId + "/annonces/2")
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isBadRequest());

        // Lecture publique + négociation de contenu
        mvc.perform(get("/api/personnes/1/lots").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/personnes/1/lots")));

        mvc.perform(get("/personnes/1/lots").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/personnes/1/lots")));

        // Retrait et suppression
        mvc.perform(delete("/api/lots/" + lotId + "/annonces/1")
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/lots/" + lotId)
                        .with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isNoContent());
    }

    @Test
    void messagesWriteLifecycleAndHalLinks() throws Exception {
        MvcResult created = mvc.perform(post("/api/messages")
                        .with(httpBasic("yanis@example.fr", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":1,\"listingId\":1,\"body\":\"Est-elle toujours disponible ?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Est-elle toujours disponible ?"))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/messages/")))
                .andExpect(jsonPath("$._links.expediteur.href", containsString("/api/personnes/2")))
                .andExpect(jsonPath("$._links.destinataire.href", containsString("/api/personnes/1")))
                .andExpect(jsonPath("$._links.annonce.href", containsString("/api/annonces/1")))
                .andExpect(jsonPath("$._links.html.href", containsString("/annonces/1")))
                .andReturn();

        long messageId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Camille et Yanis peuvent lire le message, mais pas un tiers
        mvc.perform(get("/api/messages/" + messageId)
                        .with(httpBasic("camille@example.fr", "password"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/messages/" + messageId)));

        mvc.perform(get("/api/personnes/1/messages")
                        .with(httpBasic("camille@example.fr", "password"))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/personnes/1/messages")));

        mvc.perform(get("/api/personnes/1/messages")
                        .with(httpBasic("yanis@example.fr", "password")))
                .andExpect(status().isForbidden());

        // Espace personnel HTML connecté + pagination de Mes annonces et Mes favoris
        MvcResult spaceResult = mvc.perform(get("/mon-espace").with(user("camille@example.fr")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();
        String spaceHtml = spaceResult.getResponse().getContentAsString();
        assertTrue(spaceHtml.contains("Est-elle toujours disponible ?"));
        assertFalse(spaceHtml.isBlank());
        assertTrue(spaceHtml.contains("pageAnnonces=1"));
        String annoncesCard = spaceHtml.substring(
                spaceHtml.indexOf("id=\"mes-annonces\""),
                spaceHtml.indexOf("id=\"mes-favoris\""));
        assertTrue(annoncesCard.contains("<span class=\"count-badge\">14</span>"));
        assertEquals(5, annoncesCard.split("<li>", -1).length - 1);
        assertTrue(annoncesCard.contains("Page 1 / 3"));

        MvcResult page2Result = mvc.perform(get("/mon-espace?pageAnnonces=1&pageFavoris=0&size=2")
                        .with(user("camille@example.fr")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();
        String page2Html = page2Result.getResponse().getContentAsString();
        String page2AnnoncesCard = page2Html.substring(
                page2Html.indexOf("id=\"mes-annonces\""),
                page2Html.indexOf("id=\"mes-favoris\""));
        assertEquals(2, page2AnnoncesCard.split("<li>", -1).length - 1);
        assertTrue(page2AnnoncesCard.contains("Page 2 /"));

        // Vérification de la pagination de Mes favoris
        mvc.perform(post("/api/personnes/1/favoris/1").with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/personnes/1/favoris/2").with(httpBasic("camille@example.fr", "password")))
                .andExpect(status().isOk());
        try {
            MvcResult favPageResult = mvc.perform(get("/mon-espace?pageAnnonces=0&pageFavoris=1&size=1")
                            .with(user("camille@example.fr")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andReturn();
            String favPageHtml = favPageResult.getResponse().getContentAsString();
            String favCard = favPageHtml.substring(
                    favPageHtml.indexOf("id=\"mes-favoris\""),
                    favPageHtml.indexOf("Mes lots d’objets"));
            assertTrue(favCard.contains("<span class=\"count-badge\">2</span>"));
            assertEquals(1, favCard.split("<li>", -1).length - 1);
            assertTrue(favCard.contains("Page 2 / 2"));
            assertTrue(favCard.contains("pageFavoris=0"));
        } finally {
            mvc.perform(delete("/api/personnes/1/favoris/1").with(httpBasic("camille@example.fr", "password")))
                    .andExpect(status().isNoContent());
            mvc.perform(delete("/api/personnes/1/favoris/2").with(httpBasic("camille@example.fr", "password")))
                    .andExpect(status().isNoContent());
        }
    }
}
