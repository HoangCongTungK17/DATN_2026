package vn.hoangtung.jobfind.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import vn.hoangtung.jobfind.domain.Company;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.repository.CompanyRepository;
import vn.hoangtung.jobfind.repository.UserRepository;

@Service
public class CompanyService {

	private final CompanyRepository companyRepository;
	private final UserRepository userRepository;
	private final DataScopeService dataScopeService;

	public CompanyService(
			CompanyRepository companyRepository,
			UserRepository userRepository,
			DataScopeService dataScopeService) {
		this.companyRepository = companyRepository;
		this.userRepository = userRepository;
		this.dataScopeService = dataScopeService;
	}

	public Company handleCreateCompany(Company c) {
		Optional<User> currentUser = this.dataScopeService.getCurrentUser();
		if (currentUser.isPresent() && this.dataScopeService.isHrOnly(currentUser.get())) {
			throw new AccessDeniedException("HR khong duoc tao cong ty");
		}
		return this.companyRepository.save(c);
	}

	public List<Company> handleGetCompany(Pageable pageable) {
		Page<Company> pageCompany = this.companyRepository.findAll(pageable);
		return pageCompany.getContent();
	}

	public Company handleUpdateCompany(Company c) {
		Optional<Company> companyOptional = this.companyRepository.findById(c.getId());

		if (companyOptional.isPresent()) {
			Company currentCompany = companyOptional.get();
			ensureCanManageCompany(currentCompany);
			currentCompany.setLogo(c.getLogo());
			currentCompany.setName(c.getName());
			currentCompany.setDescription(c.getDescription());
			currentCompany.setAddress(c.getAddress());
			return this.companyRepository.save(currentCompany);
		}
		return null;
	}

	public void handleDeleteCompany(long id) {
		Optional<Company> comOptional = this.companyRepository.findById(id);
		if (comOptional.isPresent()) {
			Company com = comOptional.get();
			ensureCanManageCompany(com);
			this.dataScopeService.getCurrentUser().ifPresent(user -> {
				if (this.dataScopeService.isHrOnly(user)) {
					throw new AccessDeniedException("HR khong duoc xoa cong ty");
				}
			});
			// fetch all user belong to this company
			List<User> users = this.userRepository.findByCompany(com);
			this.userRepository.deleteAll(users);
		}
		this.companyRepository.deleteById(id);
	}

	public Optional<Company> findById(long id) {
		return this.companyRepository.findById(id);
	}

	public Optional<Company> findByIdForAdminScope(long id) {
		User currentUser = this.dataScopeService.getCurrentUserOrThrow();
		Optional<Company> company = this.companyRepository.findById(id);
		company.ifPresent(item -> ensureCanReadCompany(item, currentUser));
		return company;
	}

	public ResultPaginationDTO handleGetCompany(Specification<Company> spec, Pageable pageable) {
		Page<Company> pCompany = this.companyRepository.findAll(spec, pageable);
		ResultPaginationDTO rs = new ResultPaginationDTO();
		ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

		mt.setPage(pageable.getPageNumber() + 1);
		mt.setPageSize(pageable.getPageSize());

		mt.setPages(pCompany.getTotalPages());
		mt.setTotal(pCompany.getTotalElements());

		rs.setMeta(mt);
		rs.setResult(pCompany.getContent());
		return rs;
	}

	public ResultPaginationDTO handleGetCompanyForAdminScope(Specification<Company> spec, Pageable pageable) {
		User currentUser = this.dataScopeService.getCurrentUserOrThrow();
		Specification<Company> scopeSpec = this.dataScopeService.companyScopeFor(currentUser);
		Specification<Company> finalSpec = spec == null ? scopeSpec : scopeSpec.and(spec);
		return handleGetCompany(finalSpec, pageable);
	}

	private void ensureCanReadCompany(Company company, User currentUser) {
		if (this.dataScopeService.isAdmin(currentUser)) {
			return;
		}
		if (this.dataScopeService.isHrOnly(currentUser)
				&& this.dataScopeService.isSameCompany(company, currentUser.getCompany())) {
			return;
		}
		throw new AccessDeniedException("Ban khong co quyen truy cap cong ty nay");
	}

	private void ensureCanManageCompany(Company company) {
		this.dataScopeService.getCurrentUser().ifPresent(user -> {
			if (this.dataScopeService.isHrOnly(user)
					&& !this.dataScopeService.isSameCompany(company, user.getCompany())) {
				throw new AccessDeniedException("HR chi duoc quan ly cong ty cua minh");
			}
		});
	}

}
