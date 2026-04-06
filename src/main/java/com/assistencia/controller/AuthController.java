package com.assistencia.controller;

import com.assistencia.model.Empresa;
import com.assistencia.model.Usuario;
import com.assistencia.repository.EmpresaRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.util.CpfValidator; // Certifique-se de criar essa classe no pacote util
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("empresas", empresaRepository.findByAtivoTrue());
        return "login";
    }

    @PostMapping("/registro")
    public String registrarFuncionario(Usuario usuario,
                                       @RequestParam(required = false) Long empresaId,
                                       RedirectAttributes attributes) {
        try {
            // 1. VALIDAÇÃO DE CPF (S.I. Quality Check)
            if (usuario.getCpf() == null || !CpfValidator.isValid(usuario.getCpf())) {
                attributes.addAttribute("invalidCpf", true);
                return "redirect:/login";
            }

            if (empresaId == null) {
                attributes.addAttribute("error", true);
                return "redirect:/login";
            }

            if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
                attributes.addAttribute("error", true);
                return "redirect:/login";
            }

            Empresa emp = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setRole("ROLE_FUNCIONARIO");
            usuario.setAprovado(false);
            usuario.setEmpresa(emp);

            usuarioRepository.save(usuario);
            attributes.addAttribute("success", true);

        } catch (Exception e) {
            attributes.addAttribute("error", true);
        }
        return "redirect:/login";
    }

    @PostMapping("/registro-empresa")
    public String registrarEmpresa(@RequestParam String nomeEmpresa,
                                   Usuario usuario,
                                   RedirectAttributes attributes) {
        try {
            // 1. VALIDAÇÃO DE CPF (Garante Pix no Mercado Pago depois)
            if (usuario.getCpf() == null || !CpfValidator.isValid(usuario.getCpf())) {
                attributes.addAttribute("invalidCpf", true);
                return "redirect:/login";
            }

            if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
                attributes.addAttribute("error", true);
                return "redirect:/login";
            }

            // Criamos e salvamos a empresa
            Empresa novaEmpresa = new Empresa();
            novaEmpresa.setNome(nomeEmpresa);
            novaEmpresa.setAtivo(true);
            novaEmpresa.setDiasRestantes(7);
            Empresa empresaSalva = empresaRepository.save(novaEmpresa);

            // Criamos o usuário vinculado
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setRole("ROLE_ADMIN");
            usuario.setTipoFuncionario("PROPRIETARIO");
            usuario.setAprovado(true);
            usuario.setEmpresa(empresaSalva);

            usuarioRepository.save(usuario);
            attributes.addAttribute("success", true);

        } catch (Exception e) {
            attributes.addAttribute("error", true);
        }
        return "redirect:/login";
    }
}