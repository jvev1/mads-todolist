package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class DescripcionUsuarioWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    public void descripcionMuestraDatosUsuarioSinPassword() throws Exception {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("descripcion@ua.es");
        usuario.setNombre("Usuario Descripcion");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados/" + usuario.getId()))
                .andExpect(content().string(allOf(
                        containsString("Descripción de usuario"),
                        containsString("descripcion@ua.es"),
                        containsString("Usuario Descripcion"),
                        not(containsString("123")))));
    }

    @Test
    public void listadoTieneEnlaceADescripcion() throws Exception {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("enlace@ua.es");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(
                        containsString("/registrados/" + usuario.getId())));
    }
}
