package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class GestionEquiposWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EquipoService equipoService;

    @MockBean
    private ManagerUserSession managerUserSession;

    private UsuarioData addUsuarioBD(String email) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setPassword("123");
        return usuarioService.registrar(usuario);
    }

    @Test
    public void getFormNuevoEquipoMuestraFormulario() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Nuevo equipo"),
                        containsString("action=\"/equipos/nuevo\""))));
    }

    @Test
    public void postNuevoEquipoCreaEquipoYRedirige() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(post("/equipos/nuevo")
                        .param("nombre", "Proyecto AAA"))
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Equipo creado correctamente"));

        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();
        assertThat(equipos).hasSize(1);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Proyecto AAA");
    }

    @Test
    public void postNuevoEquipoNombreVacioVuelveAlFormulario() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(post("/equipos/nuevo")
                        .param("nombre", "   "))
                .andExpect(redirectedUrl("/equipos/nuevo"))
                .andExpect(flash().attributeExists("error"));

        assertThat(equipoService.findAllOrdenadoPorNombre()).isEmpty();
    }

    @Test
    public void nuevoEquipoNoAccesibleSinLogin() throws Exception {
        // GIVEN
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/nuevo"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(post("/equipos/nuevo")
                        .param("nombre", "Proyecto AAA"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void postUnirmeAñadeUsuarioAlEquipo() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirme"))
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Te has unido al equipo"));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).hasSize(1);
    }

    @Test
    public void postUnirmeYaMiembroMuestraError() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirme"))
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    public void postSalirEliminaUsuarioDelEquipo() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/salir"))
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attribute("mensaje", "Has salido del equipo"));

        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
    }

    @Test
    public void postSalirNoMiembroMuestraError() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/salir"))
                .andExpect(redirectedUrl("/equipos"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    public void unirmeYSalirEquipoInexistenteDevuelveNotFound() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(post("/equipos/1/unirme"))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(post("/equipos/1/salir"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void listadoMuestraBotonUnirmeOSalirSegunPertenencia() throws Exception {
        // GIVEN
        // Un usuario que pertenece a un equipo pero no al otro
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        EquipoData equipoMiembro = equipoService.crearEquipo("Proyecto AAA");
        EquipoData equipoNoMiembro = equipoService.crearEquipo("Proyecto BBB");
        equipoService.añadirUsuarioAEquipo(equipoMiembro.getId(), usuario.getId());
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        // Aparece el botón de salir en su equipo y el de unirse en el otro
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("action=\"/equipos/" + equipoMiembro.getId() + "/salir\""),
                        containsString("action=\"/equipos/" + equipoNoMiembro.getId() + "/unirme\""),
                        not(containsString("action=\"/equipos/" + equipoMiembro.getId() + "/unirme\"")),
                        not(containsString("action=\"/equipos/" + equipoNoMiembro.getId() + "/salir\"")))));
    }

    @Test
    public void unirmeYSalirNoAccesibleSinLogin() throws Exception {
        // GIVEN
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/unirme"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(post("/equipos/" + equipo.getId() + "/salir"))
                .andExpect(status().isUnauthorized());
    }
}
